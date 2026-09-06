package wemove.platform;

import java.util.function.Supplier;

public interface UnitOfWork {
    <T> T run(Supplier<T> work);
}
