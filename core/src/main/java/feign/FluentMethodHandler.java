package feign;

import feign.InvocationHandlerFactory.MethodHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class FluentMethodHandler implements MethodHandler {

    private final MethodHandler delegate;

    public FluentMethodHandler(Target target, Proxy targetProxy) {
        this.delegate = new MethodHandler() {
            @Override
            public Object invoke(Object[] parentArgs) throws Throwable {
                return Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[]{target.type()}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object[] resultArgs = concatArrays(args);
                        return Proxy.getInvocationHandler(targetProxy).invoke(proxy, method, resultArgs);
                    }

                    private Object[] concatArrays(Object[] args) {
                        Object[] resultArgs = args;
                        if (parentArgs != null) {
                            if (args != null) {
                                int argsLength = args.length;
                                int parentArgsLength = parentArgs.length;
                                resultArgs = Arrays.copyOf(parentArgs, parentArgsLength + argsLength);
                                System.arraycopy(args, 0, resultArgs, parentArgsLength, argsLength);
                            } else {
                                resultArgs = parentArgs;
                            }
                        }
                        return resultArgs;
                    }
                });
            }
        };
    }

    @Override
    public Object invoke(Object[] argv) throws Throwable {
        return delegate.invoke(argv);
    }
}
