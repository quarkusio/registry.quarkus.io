package io.quarkus.registry.app.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.enterprise.inject.spi.CDI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class JsonbType implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.JAVA_OBJECT };
    }

    @Override
    public Class<?> returnedClass() {
        return JsonNode.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return false;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return 0;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        final String cellContent = rs.getString(names[0]);
        if (cellContent == null) {
            return null;
        }
        try {
            return getMapper().readTree(cellContent);
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to convert String to Invoice: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        JsonNode node = (JsonNode) value;
        if (node.isNull()) {
            st.setNull(index, Types.OTHER);
            return;
        }
        try {
            String strVal = getMapper().writeValueAsString(value);
            st.setObject(index, strVal, Types.OTHER);
        } catch (JsonProcessingException e) {
            throw new HibernateException("Could not convert JSON tree", e);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        ObjectMapper mapper = getMapper();
        try {
            return mapper.readTree(mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new HibernateException("Cannot deep copy", e);
        }
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return null;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return null;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public ObjectMapper getMapper() {
        return CDI.current().select(ObjectMapper.class).get();
    }
}
