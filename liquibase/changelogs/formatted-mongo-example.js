// liquibase formatted mongo

// =========================================================
// Formatted Mongo Changelog Example (Requires Pro/Secure)
// This file demonstrates rollback using the //rollback
// comment syntax in .js formatted Mongo changelogs.
// =========================================================

// changeset admin:fm-001 labels:release-2.0.0 context:all runWith:mongosh
// comment: Create categories collection via mongosh
db = db.getSiblingDB('myapp_db');
db.createCollection('categories', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['name'],
            properties: {
                name:        { bsonType: 'string' },
                description: { bsonType: 'string' }
            }
        }
    }
});
// rollback db = db.getSiblingDB('myapp_db');
// rollback db.categories.drop();

// changeset admin:fm-002 labels:release-2.0.0 context:all runWith:mongosh
// comment: Create index on categories.name
db = db.getSiblingDB('myapp_db');
db.categories.createIndex({ name: 1 }, { unique: true, name: 'idx_categories_name' });
// rollback db = db.getSiblingDB('myapp_db');
// rollback db.categories.dropIndex('idx_categories_name');

// changeset admin:fm-003 labels:release-2.0.0 context:all runWith:mongosh
// comment: Insert seed categories
db = db.getSiblingDB('myapp_db');
db.categories.insertMany([
    { _id: 'cat-001', name: 'Programming',  description: 'Software development books' },
    { _id: 'cat-002', name: 'Architecture', description: 'System design and architecture' },
    { _id: 'cat-003', name: 'DevOps',       description: 'Operations and infrastructure' }
]);
// rollback db = db.getSiblingDB('myapp_db');
// rollback db.categories.deleteMany({ _id: { $in: ['cat-001', 'cat-002', 'cat-003'] } });

// changeset admin:fm-004 labels:release-2.0.0 context:all runWith:mongosh
// comment: Add categoryId field to existing books
db = db.getSiblingDB('myapp_db');
db.books.updateMany({}, { $set: { categoryId: 'cat-001' } });
// rollback db = db.getSiblingDB('myapp_db');
// rollback db.books.updateMany({}, { $unset: { categoryId: '' } });
