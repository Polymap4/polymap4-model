/* 
 * polymap.org
 * Copyright (C) 2012-2015, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2;

import java.util.function.Consumer;
import areca.common.Promise;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface Association<T extends Entity>
        extends PropertyBase<T> {

    public Promise<T> fetch();

    public void set( T value );

    
    // Optional *******************************************
    
    
    /**
     * If a value is present then (asynchronously) invoke the specified consumer with
     * the value, otherwise do nothing.
     *
     * @param consumer block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code consumer} is null
     */
    public default Promise<T> ifPresent( Consumer<? super T> consumer ) {
        return fetch().onSuccess( value -> {
            if (value != null) {
                consumer.accept( value );
            }
        });
    }

//    /**
//     * Returns {@code true} if there is a value present, otherwise {@code false}.
//     */
//    public default boolean isPresent() {
//        return get() != null;
//    }
//
//    /**
//     * Return the value if present, otherwise return {@code other}.
//     *
//     * @param other the value to be returned if there is no value present, may be
//     *        null
//     * @return the value, if present, otherwise {@code other}
//     */
//    public default T orElse( T other ) {
//        T value = get();
//        return value != null ? value : other;
//    }
//
//    /**
//     * Return the value if present, otherwise invoke {@code other} and return
//     * the result of that invocation.
//     *
//     * @param other a {@code Supplier} whose result is returned if no value
//     * is present
//     * @return the value if present otherwise the result of {@code other.get()}
//     * @throws NullPointerException if value is not present and {@code other} is
//     * null
//     */
//    public default T orElseGet( Supplier<? extends T> other ) {
//        T value = get();
//        return value != null ? value : other.get();
//    }
//
//    /**
//     * Return the contained value, if present, otherwise throw an exception
//     * to be created by the provided supplier.
//     *
//     * @apiNote A method reference to the exception constructor with an empty
//     * argument list can be used as the supplier. For example,
//     * {@code IllegalStateException::new}
//     *
//     * @param <X> Type of the exception to be thrown
//     * @param exceptionSupplier The supplier which will return the exception to
//     * be thrown
//     * @return the present value
//     * @throws X if there is no value present
//     * @throws NullPointerException if no value is present and
//     * {@code exceptionSupplier} is null
//     */
//    public default <X extends Throwable> T orElseThrow( Supplier<? extends X> exceptionSupplier ) throws X {
//        T value = get();
//        if (value != null) {
//            return value;
//        } else {
//            throw exceptionSupplier.get();
//        }
//    }
//    
//    /**
//     * If a value is present, and the value matches the given predicate, return an
//     * {@link Optional} describing the value, otherwise return an empty Optional.
//     *
//     * @param predicate A predicate to apply to the value.
//     * @return An {@code Optional} describing the value of this {@code Optional} if a
//     *         value is present and the value matches the given predicate, otherwise
//     *         an empty {@code Optional}
//     */
//    public default Optional<T> filter( Predicate<? super T> predicate ) {
//        assert predicate != null;
//        T value = get();
//        return value != null && predicate.test( value ) ? Optional.of( value ) : Optional.empty();
//    }
//
//    /**
//     * If a value is present, apply the provided mapping function to it,
//     * and if the result is non-null, return an {@code Optional} describing the
//     * result.  Otherwise return an empty {@code Optional}.
//     *
//     * @apiNote This method supports post-processing on optional values, without
//     * the need to explicitly check for a return status.  For example, the
//     * following code traverses a stream of file names, selects one that has
//     * not yet been processed, and then opens that file, returning an
//     * {@code Optional<FileInputStream>}:
//     *
//     * <pre>{@code
//     *     Optional<FileInputStream> fis =
//     *         names.stream().filter(name -> !isProcessedYet(name))
//     *                       .findFirst()
//     *                       .map(name -> new FileInputStream(name));
//     * }</pre>
//     *
//     * Here, {@code findFirst} returns an {@code Optional<String>}, and then
//     * {@code map} returns an {@code Optional<FileInputStream>} for the desired
//     * file if one exists.
//     *
//     * @param <U> The type of the result of the mapping function
//     * @param mapper a mapping function to apply to the value, if present
//     * @return an {@code Optional} describing the result of applying a mapping
//     * function to the value of this {@code Optional}, if a value is present,
//     * otherwise an empty {@code Optional}
//     * @throws NullPointerException if the mapping function is null
//     */
//    public default <U> Optional<U> map( Function<? super T, ? extends U> mapper ) {
//        assert mapper != null;
//        T value = get();
//        return value != null ? Optional.ofNullable( mapper.apply( value ) ) : Optional.empty();
//    }

}
