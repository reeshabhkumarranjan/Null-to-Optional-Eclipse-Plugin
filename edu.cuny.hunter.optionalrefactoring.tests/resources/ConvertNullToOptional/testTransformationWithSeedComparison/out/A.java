package p;

import java.util.Optional;

public class A {

    Optional<String> o = Optional.of("Hi");
    boolean b = !o.isPresent();
}