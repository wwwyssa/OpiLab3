package com.example.laba4.auth;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@Repository
public class UserRepository {
    private final DSLContext dsl;
    private static final Table<?> USERS = table(name("users"));
    private static final Field<Long> ID = field(name("id"), Long.class);
    private static final Field<String> USERNAME = field(name("username"), String.class);
    private static final Field<String> PASSWORD_HASH = field(name("password_hash"), String.class);

    public record UserData(Long id, String username, String passwordHash) {}

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<UserData> findByUsername(String username) {
        Record rec = dsl.select(ID, USERNAME, PASSWORD_HASH)
                .from(USERS)
                .where(USERNAME.eq(username))
                .fetchOne();

        if (rec == null) {
            return Optional.empty();
        }

        return Optional.of(new UserData(rec.get(ID), rec.get(USERNAME), rec.get(PASSWORD_HASH)));
    }

    public UserData create(String username, String passwordHash) {
        Record rec = dsl.insertInto(USERS)
                .set(USERNAME, username)
                .set(PASSWORD_HASH, passwordHash)
                .returning(ID, USERNAME, PASSWORD_HASH)
                .fetchOne();

        if (rec == null) {
            throw new IllegalStateException("Failed to create user record");
        }

        return new UserData(rec.get(ID), rec.get(USERNAME), rec.get(PASSWORD_HASH));
    }
}
