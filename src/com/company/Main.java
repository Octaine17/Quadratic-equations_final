package com.company;

import javafx.util.Pair;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    enum FuncType {
        NoException, Normal, FullException
    }

    public static class ThrowableVector extends Throwable {
        Vector<Double> vector = new Vector<Double>();
    }

    static boolean isEqual(double a, double b) {
        double eps = 0.00001;
        return Math.abs(a - b) < eps;
    }

    static Vector<Double> solve_correct_equation(double a, double b, double c) {
        assert (!(isEqual(a, 0) && isEqual(b, 0) && isEqual(c, 0)));
        if (isEqual(a, 0) && isEqual(b, 0)) {
            return new Vector<Double>(0);
        }

        if (isEqual(a, 0)) {
            Vector<Double> vec = new Vector<Double>(1);
            vec.add(-c / b);
            return vec;
        }

        double discriminant = (b * b) - (4 * a * c);
        if (isEqual(discriminant, 0)) {
            Vector<Double> vec = new Vector<Double>(1);
            vec.add(-b / (2 * a));
            return vec;
        }
        if (discriminant < 0) {
            return new Vector<Double>(0);
        }
        Vector<Double> vec = new Vector<Double>(2);
        vec.add((-b + Math.sqrt(discriminant)) / (2 * a));
        vec.add((-b - Math.sqrt(discriminant)) / (2 * a));
        return vec;
    }


    static Object solve_no_exceptions(double a, double b, double c, boolean ok) {
        ok = true;
        if (isEqual(a, 0) && isEqual(b, 0) && isEqual(c, 0)) {
            ok = false;
            return new Vector<Double>();
        }
        return new Pair<>(ok, solve_correct_equation(a, b, c));
    }

    static double call_solver(FuncType type, double a, double b, double c) {
        switch (type) {
            case Normal:
                return roots_sum(a, b, c);
            case NoException:
                return roots_sum_no_exception(a, b, c);
            case FullException:
                return roots_sum_full_exception(a, b, c);
            default:
                return 0;
        }
    }

    static double roots_sum(double a, double b, double c) {
        try {
            Vector<Double> roots = solve(a, b, c);
            return sum_vec(roots);
        } catch (RuntimeException runtimeException) {
            return 0;
        }
    }

    static Vector<Double> solve(double a, double b, double c) {
        if (isEqual(a, 0) && isEqual(b, 0) && isEqual(c, 0)) {
            throw new RuntimeException("root is any value");
        }
        return solve_correct_equation(a, b, c);
    }

    static double roots_sum_no_exception(double a, double b, double c) {
        Pair pair = (Pair) solve_no_exceptions(a, b, c, true);
        if (true == (boolean) pair.getKey()) {
            return sum_vec((Vector<Double>) pair.getValue());
        }
        return 0;
    }

    static double sum_vec(Vector<Double> roots) {
        double sum = 0;
        for (int i = 0; i < roots.size(); i++) {
            sum += roots.elementAt(i);
        }
        return sum;
    }

    public static double sum_vec(ThrowableVector roots) {
        double sum = 0;
        for (int i = 0; i < roots.vector.size(); i++) {
            sum += roots.vector.elementAt(i);
        }
        return sum;
    }

    static double roots_sum_full_exception(double a, double b, double c) {
        try {
            solve_full_exception(a, b, c);
        } catch (RuntimeException s) {
            return 0;
        } catch (ThrowableVector vector) {
            return sum_vec(vector);
        }
        throw new RuntimeException("wrong exception");
    }

    static void solve_full_exception(double a, double b, double c) throws ThrowableVector {
        if (isEqual(a, 0) && isEqual(b, 0) && isEqual(c, 0)) {
            throw new RuntimeException("root is any value");
        }
        ThrowableVector vec = new ThrowableVector();
        vec.vector = (solve_correct_equation(a, b, c));
        throw vec;
    }


    public static void main(String[] args) {
        final int from = 1024;
        final int to = from * 512;
        System.out.println("\t\t\t--------Normal-----------");
        for (long i = from; i <= to; i *= 2) {
            run(i, FuncType.Normal);
        }
        System.out.println("\t\t\t--------No exception-----------");
        for (long i = from; i <= to; i *= 2) {
            run(i, FuncType.NoException);
        }
        System.out.println("\t\t\t--------FullException-----------");
        for (long i = from; i <= to; i *= 2) {
            run(i, FuncType.FullException);
        }
    }

    static void run(long n, FuncType type) {
        long time = System.nanoTime();
        double sum = 0;
        for (long i = 0; i < n; i++) {
            double a = ((i % 2000) - 1000) / 33.0;
            double b = ((i % 200) - 100) / 22.0;
            double c = ((i % 20) - 10) / 11.0;
            sum += call_solver(type, a, b, c);
        }
        time = System.nanoTime() - time;
        System.out.printf("Elapsed Consecutively %,9.3f ms\n", time / 1_000_000.0);
        time = System.nanoTime();
        AtomicReference<Double> final_sum = new AtomicReference<>((double) 0);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Lock lock = new ReentrantLock();
        executor.submit(() -> {
            double temp_sum = 0;
            for (long i = 0; i < n; i++) {
                double a = ((i % 2000) - 1000) / 33.0;
                double b = ((i % 200) - 100) / 22.0;
                double c = ((i % 20) - 10) / 11.0;
                temp_sum += call_solver(type, a, b, c);
            }
            lock.lock();
            try {
                double finalTemp_sum = temp_sum;
                final_sum.updateAndGet(v -> new Double((double) (v + finalTemp_sum)));
            } finally {
                lock.unlock();
            }
            executor.shutdown();
        });
        time = System.nanoTime() - time;
        System.out.printf("Elapsed Parrarel      %,9.3f ms\n", time / 1_000_000.0);
    }
}