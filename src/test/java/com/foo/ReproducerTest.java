package com.foo;

import com.antwerkz.bottlerocket.BottleRocket;
import com.antwerkz.bottlerocket.BottleRocketTest;
import com.github.zafarkhaja.semver.Version;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.query.ValidationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.function.Supplier;

import static dev.morphia.query.filters.Filters.eq;

import static java.lang.String.format;
import static org.junit.Assert.*;

public class ReproducerTest extends BottleRocketTest {
    private Datastore datastore;
    private ReferenceDocument ref1, ref2;
    private RootDocument doc1, doc2;

    public ReproducerTest() {
        MongoClient mongo = getMongoClient();
        MongoDatabase database = getDatabase();
        database.drop();
        datastore = Morphia.createDatastore(mongo, getDatabase().getName());
    }

    @NotNull
    @Override
    public String databaseName() {
        return "morphia_repro";
    }

    @Nullable
    @Override
    public Version version() {
        return BottleRocket.DEFAULT_VERSION;
    }

    @Test
    public void reproduce() {
        setUp();
        try {
            callFirstWithDollarId(); // At first an exception is thrown, then the CORRECT record is fetched.
            callToStringWithDollarId(); // At first an exception is thrown, then the CORRECT record is fetched.
            callFirstWithUnderscoreIdId(); // At first an exception is thrown, then NO data is fetched.
            callFirstWithId(); // At first an exception is thrown, then NO data is fetched.
            callFirstWithoutId(); // No exception is ever thrown, but NO data is fetched.
        } finally {
            tearDown();
        }
    }

    private void setUp() {
        ref2 = new ReferenceDocument();
        ref2.id = 3;
        datastore.save(ref2);

        doc2 = new RootDocument();
        doc2.id = 4;
        doc2.ref = ref2;
        datastore.save(doc2);

        ref1 = new ReferenceDocument();
        ref1.id = 2;
        datastore.save(ref1);

        doc1 = new RootDocument();
        doc1.id = 1;
        doc1.ref = ref1;
        datastore.save(doc1);

    }

    private void tearDown() {
        datastore.delete(doc1);
        datastore.delete(ref1);
        datastore.delete(doc2);
        datastore.delete(ref2);
    }

    public void callFirstWithDollarId() {
        var field = "ref.$id";
        var query = datastore.find(RootDocument.class)
                .filter(eq(field, 2));
        giveItATry(field, query::first);  // First attempt at calling first: an exception is thrown.
        var result = query.first(); // Second attempt: everything is fine and the expected record is fetched.
        assertNotNull(result);
        assertEquals(1, result.id);
        result = query.first(); // Third attempt: yet again everything is fine.
        assertNotNull(result);
        assertEquals(1, result.id);
    }

    public void callToStringWithDollarId() {
        var field = "ref.$id";
        var query = datastore.find(RootDocument.class)
                .filter(eq(field, 2));
        giveItATry(field, query::toString);  // At first, try to print the query: an exception is thrown.
        var result = query.first(); // First attempt at calling first: everything is fine and the expected record is fetched.
        assertNotNull(result);
        assertEquals(1, result.id);
        result = query.first(); // Second attempt at calling first: yet again everything is fine.
        assertNotNull(result);
        assertEquals(1, result.id);
    }

    public void callFirstWithUnderscoreIdId() {
        var field = "ref._id";
        var query = datastore.find(RootDocument.class)
                .filter(eq(field, 2));
        giveItATry(field, query::first);  // First attempt at calling first: an exception is thrown.
        var result = query.first(); // Second attempt: the query runs without issues, but no data is fetched.
        assertNull(result);
        result = query.first(); // Third attempt: still no data.
        assertNull(result);
    }

    public void callFirstWithId() {
        var field = "ref.id";
        var query = datastore.find(RootDocument.class)
                .filter(eq(field, 2));
        giveItATry(field, query::first);  // First attempt at calling first: an exception is thrown.
        var result = query.first(); // Second attempt: the query runs without issues, but no data is fetched.
        assertNull(result);
        result = query.first(); // Third attempt: still no data.
        assertNull(result);
    }

    public void callFirstWithoutId() {
        var field = "ref";
        var query = datastore.find(RootDocument.class)
                .filter(eq(field, 2));
        var result = query.first(); // First attempt at calling first: no error is ever thrown, but no data is fetched.
        assertNull(result);
        result = query.first(); // Second attempt: still no data.
        assertNull(result);
    }

    private <T> void giveItATry(String field, Supplier<T> supplier) {
        var message = "";
        try {
            supplier.get();
        } catch (ValidationException e) {
            e.printStackTrace();
            message = e.getMessage();
        }
        assertEquals(
                format("Could not resolve path '%s' against '%s'.  Unknown path element: 'ref'.", field, RootDocument.class.getName()),
                message);
    }

}
