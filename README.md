# Mini Redis Java

Mini Redis Java is an educational in-memory key-value store inspired by Redis.
The goal of this project is to understand how a simple storage engine works under
the hood by implementing the core data structure manually in Java.

This project is not a production-ready Redis clone. It is a learning project
focused on hash tables, collision handling, linked nodes, and key-value storage.

## Tech Stack

- Java 17
- Maven
- Object-oriented programming
- Manual hash table implementation

## What This Project Does

The current version implements a custom hash table that can:

- Store values by key
- Retrieve values by key
- Update existing keys
- Remove keys
- Check if a key exists
- Track the current number of stored entries
- Handle hash collisions using linked nodes

Example:

```java
MiniHashTable table = new MiniHashTable();

table.put("name", new Entry("Joao"));
table.put("age", new Entry("20"));

System.out.println(table.get("name").getValue()); // Joao

table.put("name", new Entry("Joao Pedro"));

System.out.println(table.get("name").getValue()); // Joao Pedro

table.remove("age");

System.out.println(table.containsKey("age")); // false
```

## Internal Structure

The project is built around three main classes:

```text
MiniHashTable
  Manages the bucket array and decides where each key should be stored.

HashNode
  Represents one item inside a bucket. It stores the key, the Entry value,
  and a reference to the next node when collisions happen.

Entry
  Represents the value stored in the database. It currently stores the string
  value and expiration metadata.
```

The storage flow looks like this:

```text
key -> hash(key) -> bucket index -> HashNode -> Entry
```

For example:

```text
"name" -> hash("name") -> index 3 -> HashNode("name") -> Entry("Joao")
```

## Why Entry Exists

Instead of storing only a plain `String`, the hash table stores an `Entry`.

That makes it possible to keep extra metadata about a value, such as expiration
time:

```text
Entry
  value = "Joao"
  expiresAt = null
```

This is useful because Redis-like systems often need to know not only what the
value is, but also whether that value is still valid.

## How Collisions Are Handled

Two different keys can generate the same bucket index. When that happens, the
project stores multiple `HashNode` objects in the same bucket using a linked
list.

Example:

```text
buckets[3] -> HashNode("name") -> HashNode("city") -> null
```

This technique is called separate chaining.

## Project Structure

```text
src/main/java/com/joaopedro/miniredis
  Main.java
  core/
    Entry.java
    hash/
      HashNode.java
      MiniHashTable.java
```

## How To Run

Requirements for the recommended setup:

- Java 17+
- Maven

Compile the project with Maven:

```bash
mvn compile
```

Run the demo class:

```bash
java -cp target/classes com.joaopedro.miniredis.Main
```

If Maven is not installed, you can compile the project directly with `javac`:

```bash
javac -d target/classes src/main/java/com/joaopedro/miniredis/Main.java src/main/java/com/joaopedro/miniredis/core/Entry.java src/main/java/com/joaopedro/miniredis/core/hash/HashNode.java src/main/java/com/joaopedro/miniredis/core/hash/MiniHashTable.java
```

## Current Learning Goals

This project is helping me practice:

- How hash tables work internally
- How keys are converted into array indexes
- How collisions can be solved with linked lists
- How to separate responsibilities between classes
- How Redis-like key-value storage works at a basic level

## Next Steps

Planned improvements:

- Add `EXPIRE` support
- Add `TTL` support
- Automatically ignore or remove expired entries
- Add resizing and rehashing when the table grows
- Add unit tests
- Create a simple command-line interface with Redis-like commands such as:

```text
SET name Joao
GET name
DEL name
EXPIRE name 10
TTL name
```

## Status

Work in progress. The basic hash table storage is implemented, and Redis-like
features will be added gradually.
