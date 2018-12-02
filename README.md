# Model2 - domain modeling for Java

The Model2 System helps to work with [Domain Models](http://en.wikipedia.org/wiki/Domain_model) and [Domain-driven Design](http://en.wikipedia.org/wiki/Domain-driven_design) (DDD) in Java. It allows to describe the various entities, their attributes, roles, and relationships, plus the constraints and concerns that govern the problem in pure Java syntax. No pre-processors or new language elements, just the standard Java platform. Everything you know from Java still applies and you can leverage both your experience and toolkits.

This work is heavily inspired by [Qi4j](http://qi4j.org/).

The runtime system allows to **plug-in** concrete implementation of **persistent store** backend, **query/index** backend, **locking** strategies and **cache** provider.

## Example

Create and configure a **repository**:

```java
repo = EntityRepository.newConfiguration()
        .store.set( new RecordStoreAdapter( new LuceneRecordStore() ) )
        .entities.set( new Class[] {Person.class} )
        .create();
```

The Person **Entity** class:
```java
@Description( "Everything we know about an employee" )
public class Person extends Entity {

    /** Simple property of type String, value might be null. */
    @Nullable
    public Property<String>      name;

    /** Firstname, must not be null, defaults to "Ulli" is not initialized. */
    @DefaultValue("Ulli")
    public Property<String>      firstname;

    /** Protected property of type Date. Might be null. */
    @Nullable
    protected Property<Date>     birthday;
    
    /** Associates other employees. */
    ManyAssociation<Employee>    worksWith;
}
```

**Create an entity** of type Person in the repository:
```java
try (UnitOfWork uow = repo.newUnitOfWork()) {
    Person person = uow.createEntity( Person.class, null, (Person prototyp) -> {
            prototyp.name.set( "Model2" );
            return prototyp;
    });
    // persistently write changes to the backend store
    uow.commit();
}
```

**Access** properties of the newly created Entity:
```java
System.out.println( "The name of the person is " + person.name.opt().orElse( "No name." ) );
```
