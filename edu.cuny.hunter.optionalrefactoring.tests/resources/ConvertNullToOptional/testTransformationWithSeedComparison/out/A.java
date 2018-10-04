package p;

import java.util.Optional;

public class A {

    Optional<Object> o = Optional.ofNullable(new Object());
    boolean b = o.isPresent();
}