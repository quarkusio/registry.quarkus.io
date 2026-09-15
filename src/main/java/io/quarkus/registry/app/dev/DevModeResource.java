package io.quarkus.registry.app.dev;

import java.io.IOException;
import java.nio.file.Files;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.runtime.StartupEvent;

/**
 * A landing page, offered only under {@code quarkus:dev}, with a button that loads a catalog into the registry.
 * <p>
 * A freshly started dev instance has an empty database, so every client endpoint answers with nothing and there is no
 * way to see whether a change works without first getting data in.
 * <p>
 * The button posts to {@code /admin/v1/extension/catalog}, the same endpoint the real publishing tooling uses.
 * This avoids trying to mimic the result of prod import paths in an {@code import.sql}.
 * <p>
 * {@link IfBuildProfile} removes the bean outside dev, so these paths do not exist in a production build.
 */
@IfBuildProfile("dev")
@ApplicationScoped
@Path("/dev")
public class DevModeResource {

    /**
     * The catalog to offer, as a path relative to the directory the registry was started from.
     * <p>
     * It can be overridden for other catalogs:
     * {@code ./mvnw quarkus:dev -Dregistry.dev.catalog=/tmp/my-catalog.json}.
     */
    static final String DEFAULT_CATALOG = "src/test/resources/extension-catalog-community.json";

    private static final String PAGE_PATH = "/dev";

    @ConfigProperty(name = "quarkus.http.port")
    int port;

    @ConfigProperty(name = "registry.dev.catalog", defaultValue = DEFAULT_CATALOG)
    String catalogPath;

    /**
     * Points people at the page on startup, but only while there is nothing in the registry to look at. Once a catalog
     * has been imported the hint is just noise on every live reload.
     */
    void announce(@Observes StartupEvent event) {
        if (isEmpty()) {
            Log.infof("The registry is empty. Open http://localhost:%d%s to import a catalog.", port, PAGE_PATH);
        }
    }

    private boolean isEmpty() {
        try {
            return QuarkusTransaction.requiringNew().call(() -> PlatformRelease.count() == 0);
        } catch (RuntimeException e) {
            // Not worth failing startup over a log line; if the schema is not ready yet, just stay quiet.
            Log.debugf(e, "Could not work out whether the registry is empty");
            return false;
        }
    }

    /**
     * The page asks for the admin token in a field rather than being given it here. An exported {@code TOKEN}
     * environment variable overrides {@code %dev.TOKEN}, and the documented way to publish is
     * {@code --token=$TOKEN} with a shared secret, so rendering the configured value would sometimes write a real
     * credential into a page that then lives in the browser cache, in view-source and in any screenshot of it.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String page() {
        return PAGE.replace("__CATALOG__", catalogPath);
    }

    @GET
    @Path("/catalog.json")
    @Produces(MediaType.APPLICATION_JSON)
    public String catalog() throws IOException {
        java.nio.file.Path cwd = java.nio.file.Path.of("").toAbsolutePath();
        java.nio.file.Path path = java.nio.file.Path.of(catalogPath).toAbsolutePath().normalize();
        if (!path.startsWith(cwd)) {
            throw new NotFoundException("Catalog path must be inside the project directory.");
        }
        if (!Files.isReadable(path)) {
            // Almost always because dev mode was started from somewhere other than the project directory.
            throw new NotFoundException(path
                    + " is not readable. Start dev mode from the project directory, or point"
                    + " -Dregistry.dev.catalog at a catalog of your own.");
        }
        return Files.readString(path);
    }

    private static final String PAGE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="utf-8">
            <title>Quarkus Extension Registry (dev)</title>
            <style>
              body { font-family: system-ui, sans-serif; max-width: 48rem; margin: 3rem auto; padding: 0 1rem;
                     line-height: 1.5; color: #1a1a1a; }
              h1 { font-size: 1.5rem; }
              h2 { font-size: 1.1rem; margin-top: 2rem; }
              .badge { background: #4695eb; color: #fff; border-radius: 0.25rem; font-size: 0.75rem;
                       padding: 0.15rem 0.5rem; vertical-align: middle; }
              button { background: #4695eb; color: #fff; border: 0; border-radius: 0.25rem; cursor: pointer;
                       font-size: 1rem; padding: 0.6rem 1.2rem; }
              button:disabled { background: #999; cursor: default; }
              label { display: block; margin: 1rem 0; }
              input[type=text] { font-family: ui-monospace, monospace; font-size: 0.9rem; padding: 0.3rem; }
              .hint { color: #555; font-size: 0.9rem; }
              pre { background: #f4f4f4; border-radius: 0.25rem; padding: 1rem; overflow-x: auto; white-space: pre-wrap; }
              code, pre { font-family: ui-monospace, monospace; font-size: 0.9rem; }
              a { color: #4695eb; }
            </style>
            </head>
            <body>
            <h1>Quarkus Extension Registry <span class="badge">dev mode</span></h1>
            <p>This page exists only under <code>quarkus:dev</code>. A fresh dev instance starts with an empty
            database, so the client endpoints have nothing to return until something is imported.</p>

            <h2>Import a catalog</h2>
            <p>Posts <code>__CATALOG__</code> to <code>/admin/v1/extension/catalog</code> &mdash; the same endpoint the
            real publishing tooling uses, so this exercises the actual import path. Safe to press more than once:
            re-importing a release replaces what it declared. Point <code>-Dregistry.dev.catalog</code> at another file
            to import that instead.</p>
            <label>Admin token
              <input type="text" id="token" value="test" size="24">
              <span class="hint">The <code>%dev.TOKEN</code> from <code>application.properties</code>. Change it if
              you started dev mode with a different one.</span>
            </label>
            <p><button id="import">Import catalog</button></p>
            <pre id="out">Nothing imported yet.</pre>

            <h2>Then look at</h2>
            <ul>
              <li><a href="/client/categories/all">/client/categories/all</a></li>
              <li><a href="/client/extensions/all">/client/extensions/all</a></li>
              <li><a href="/client/platforms">/client/platforms</a></li>
              <li><a href="/q/swagger-ui/">Swagger UI</a> for everything else</li>
            </ul>

            <script>
            const out = document.getElementById('out');
            const button = document.getElementById('import');

            button.addEventListener('click', async () => {
              button.disabled = true;
              out.textContent = 'Importing\\u2026';
              try {
                const response = await fetch('/dev/catalog.json');
                const catalog = await response.text();
                if (!response.ok) {
                  out.textContent = 'Could not read the catalog: HTTP ' + response.status + '\\n' + catalog;
                  return;
                }
                const imported = await fetch('/admin/v1/extension/catalog', {
                  method: 'POST',
                  headers: {
                    'Content-Type': 'application/json',
                    'TOKEN': document.getElementById('token').value,
                    // The groupId of the catalog's own id, which is what the publishing tooling sends. Reading it
                    // rather than hard-coding io.quarkus.platform, since -Dregistry.dev.catalog may be another one.
                    // Strip anything that is not a safe Maven groupId character to prevent header injection.
                    'X-Platform': JSON.parse(catalog).id.split(':')[0].replace(/[^\\w.\\-]/g, '')
                  },
                  body: catalog
                });
                const body = await imported.text();
                if (imported.status === 403) {
                  // The admin filter answers a bad token with an empty 403, which on its own explains nothing.
                  out.textContent = 'HTTP 403. Check the admin token above against the TOKEN dev mode started with.';
                  return;
                }
                if (!imported.ok) {
                  out.textContent = 'Import failed: HTTP ' + imported.status + '\\n' + body;
                  return;
                }
                out.textContent = 'Imported ' + body + '\\n\\n' + await summary();
              } catch (e) {
                out.textContent = 'Import failed: ' + e;
              } finally {
                button.disabled = false;
              }
            });

            async function summary() {
              const categories = (await (await fetch('/client/categories/all')).json()).categories || [];
              const extensions = (await (await fetch('/client/extensions/all')).json()).extensions || [];
              return extensions.length + ' extensions, ' + categories.length + ' categories:\\n'
                  + categories.map(c => '  ' + c.id).join('\\n');
            }
            </script>
            </body>
            </html>
            """;
}
