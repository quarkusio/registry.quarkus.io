# Categories

Notes on how categories get into the registry, and what to watch out for when changing that. Written alongside
[quarkusio/quarkus#55981](https://github.com/quarkusio/quarkus/issues/55981) and the wider
[quarkusio/quarkus#55974](https://github.com/quarkusio/quarkus/issues/55974).

## Where categories come from

A category is not a thing the registry curates. It is part of the extension catalog a platform publishes, and the only
place its name, description and metadata ever come from is that catalog — ultimately Quarkus'
`devtools/bom-descriptor-json/src/main/resources/catalog-overrides.json`.

So that is how they are stored: `AdminService.onExtensionCatalogImport` writes the catalog's category list verbatim onto
`PlatformRelease.categories`, a JSON column holding `PlatformCategory` records. Re-importing a release replaces the
list, so a category a platform has dropped does not linger. A category cannot drift from the platform that defines it,
because it has no existence apart from it.

Two consequences worth remembering:

- A new category only appears after the next platform catalog is posted to `/admin/v1/extension/catalog`. Nothing
  backfills.
- Each release owns its own copy of every definition it declared. `PATCH
  /admin/v1/platform-release/{platformKey}/{streamKey}/{version}/category/{categoryKey}` therefore edits one release's
  copy and leaves every other release alone.

### What this replaced

There used to be a `category` table and a `platform_release_category` join table. `V2__Add_categories.sql` seeded the
former once, in 2020, and `V14__Introduce_Category_key.sql` gave those rows their `category_key`. Nothing updated the
seed since, so it drifted: it was missing `ai` and `packaging`, and it still carried `grpc`, which no platform declares
any more. Worse, the import path looked categories up by key and silently skipped any it did not find, so the frozen
2020 list was also a filter on everything published afterwards.

`V20__Move_categories_to_platform_release.sql` backfills the JSON column from the join table and drops both tables.
`MigrateCategoriesTest` stands up the pre-V20 schema and exercises that backfill, because on a fresh database — which is
every database except production — there is nothing for it to move.

The `create table` statements in V1 and V16 are applied history and stay where they are; removing them would only change
what a fresh database looks like, and would make dev and test diverge from production. Getting them out of source
control entirely needs a baseline squash of the whole migration set, which is a separate job.

## What the client endpoints return

`/client/categories/all`, `/client/extensions/all` and `/client/non-platform-extensions` all serve the same list, built
by `DatabaseRegistryClient.addAllCategories`:

- the union of what **every listed platform release** declared, deduplicated by id;
- newest release first, so a renamed or reworded category shows its current definition rather than whatever an ancient
  release called it;
- within a release, the order the catalog listed them in, which is the order consumers render.

This is a deliberate choice over "just the latest core release". The union keeps serving a category that older releases
still reference, at the cost of listing some a current platform has moved on from.

Note that in production the Java `RegistryClient` never sees any of this for platform catalogs.
`quarkus.registry.platform.extension-catalog-included` is only set under `%test`, so `PlatformCatalogContentProvider`
does not fire, the registry 404s the platform descriptor, and the client resolves the real one from Maven Central. The
registry's own category list is surfaced only through the REST endpoints above. That is why the client already showed a
fuller list than the registry did.

## The two questions people ask about a category

#55974 asks for "an extra field to show if a category is in the 'official' list". That is really two questions, and
only one of them needs a field.

### Is it platform-declared? Check the description

A category with a description was declared by a platform catalog, because that is the only thing that ever writes one.
Anything else — an id claimed by an extension, a bare reference used to pin extensions to a category — arrives with
nothing but an id, and `PlatformCategory.from` deliberately leaves the description null rather than inventing
something to fill the gap.

So "is this a curated, official category?" is `description != null`. No stored boolean, no computed link, nothing to
keep in step on a write path. Today it is trivially true of everything the endpoints return, since only platform
catalogs can write a category at all; it starts doing real work the moment non-platform extensions can create them.

The corollary is a rule for whoever builds that. **Do not synthesise a description.** It is tempting to fill one in
from the name, or the id, to make a UI look tidier. That would quietly delete the only thing separating a curated
category from a typo.

### Is anything using it? That is `in-use`

Whether a category has any extensions in it is a different question, and the answer is not in the definition — it is in
the extensions. `addAllCategories` puts an `in-use` boolean in each category's metadata, computed by
`PlatformRelease.findCategoryIdsInUse()`. Extensions record their categories as a bare list of ids in their metadata,
so that query unions `platform_extension.metadata` and `extension_release.metadata` and pulls the ids out with
`jsonb_array_elements_text`. Native SQL, because the ids live inside a JSON document; the app only ever runs on
PostgreSQL.

It is computed per request rather than stored, so it cannot go stale. Practically it answers "would filtering by this
category return anything", which is what a UI wants before it renders a category chip.

A richer variant was tried and dropped: tagging each category with the release whose catalog declared it, so a consumer
could draw the "official" line wherever it liked. It costs an origin-id contract shared with extension `origins`, and
the description check already answers the question it was built for.

### They really are different sets

- Platforms **declare** categories nothing has adopted yet: described, not in use.
- Extensions **claim** categories no platform declares: in use, no description. Those are deliberately *not* listed by
  the endpoints today, because the registry has no name or description to show for them.

Do not add a stored "official" boolean for either one. It is denormalised state that has to be kept correct on every
write path, and both answers are already derivable from what is there.

### Variations you may want

- **Only categories some platform declares** is the default, and needs no filter.
- **Core platforms only?** Filter the releases on `platformStream.platform.platformType = 'C'`
  (cf. `PlatformRelease.findAllCorePlatforms`) to exclude categories contributed by third-party platforms.
- **Just one platform's category list?** Don't aggregate at all — load the release and read its `categories`. For the
  current one, `PlatformRelease.findLatest(PlatformRelease.findLatestQuarkusCore())`.

## Before letting non-platform extensions create categories

Non-platform extensions currently have nowhere to put a category definition: `categories` hangs off `PlatformRelease`,
and a standalone extension has no release to hang it from. Giving them one is a schema decision in its own right. Two
things should land *with* that change, not after it.

### 1. Decide who is authoritative for name and description

While only platform catalogs write categories, taking the incoming catalog's name and description verbatim is right.
Once extensions can write, an extension must not be able to rewrite the curated name of `ai`, so the write path needs to
distinguish its callers: platform catalogs authoritative, extensions create-only.

`PlatformCategory.from` falls back to the id when the catalog supplies no name, which covers the common case, because
extension metadata categories are bare id strings and `getName()` comes back null. It will not save you if you
synthesise a `catalog.Category` with `setName(id)` as a convenience — that would replace "Artificial Intelligence (AI)"
with "ai". The same applies to the description, where it matters more, because a null description is what marks a
category as not platform-declared.

### 2. Normalise the key on create

Extension metadata categories in the wild are inconsistently cased; #55974 calls this out. Every writer today is a
platform descriptor with already-normalised ids, so there is nothing to normalise and no way to tell. The moment
extensions can write, `ai` and `Artificial Intelligence` become two separate categories.

This is the one place where acting early pays: adding normalisation now costs nothing and changes nothing, whereas
adding it later means migrating data that already exists. Note that the old `category_key` unique constraint is gone
along with the table, so there is no longer a database-level backstop.

## Ordering

`PlatformRelease.categories` is a list, and JSON preserves list order, so a release stores its categories in exactly the
order its catalog listed them. The working assumption is that platforms order them deliberately and consumers render
them in that order; alphabetical would also be a reasonable choice, but it would be a change.
`PlatformCatalogContentProviderTest` compares the round-tripped list order-sensitively against the fixture.
