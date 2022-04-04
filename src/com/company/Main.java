package com.company;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    enum FuncType {
        NoException, Normal, FullException
    }

    enum ProcessType {
        Sequence, Paraller
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


    static Pair<Boolean, Vector<Double>> solve_no_exceptions(double a, double b, double c) {
        if (isEqual(a, 0) && isEqual(b, 0) && isEqual(c, 0)) {
            return new Pair<>(false, new Vector<Double>());
        }
        return new Pair<>(true, solve_correct_equation(a, b, c));
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
       if (isEqual(a, 0) && isEqual(b, 0) && isEqual(c, 0)) {
            return new Pair(false, new Vector<Double>());
        }
        return new Pair(true, solve_correct_equation(a,b,c));
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
        final int from = 32768;
        final int to = 2097152;
        System.out.println("\t\t\t--------Normal-----------");
        for (long i = from; i <= to; i *= 2) {
            run(i, FuncType.Normal, ProcessType.Paraller);
        }
        System.out.println("\t\t\t--------No exception-----------");
        for (long i = from; i <= to; i *= 2) {
            run(i, FuncType.NoException, ProcessType.Paraller);
        }
        System.out.println("\t\t\t--------FullException-----------");
        for (long i = from; i <= to; i *= 2) {
            run(i, FuncType.FullException, ProcessType.Paraller);
        }
    }

    static void run(long n, FuncType type, ProcessType processType) {
        System.gc();
        long time = System.nanoTime();
        if (processType == ProcessType.Sequence) {
            Double sum = run_sequence(0, n, type);
            System.out.println("Sum =" + sum);
        } else if (processType == ProcessType.Paraller) {
            run_parallel(n, type);
        }
        time = System.nanoTime() - time;
        System.out.printf( n + " %,9.3f ms\n", time / 1_000_000.0);
    }

    public static void run_parallel(long n, FuncType type) {
        final int thread_num = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(thread_num);
        List<Callable<Double>> tasks = new ArrayList<>();
        try {
            long data_per_thread = n / thread_num;
            for (int thread_id = 0; thread_id < thread_num; ++thread_id){
                final int thread_id_copy = thread_id;
                long start = thread_id_copy * data_per_thread;
                long end = start + data_per_thread;
                long finalEnd = end;
                if (thread_id_copy == thread_num - 1) {
                    end = n;
                }
                tasks.add(new Callable<Double>() {
                    @Override
                    public Double call() throws Exception {
                        return run_sequence(start, finalEnd,type);
                    }
                });

        } List<Future<Double>> invokeAll = executor.invokeAll(tasks);

        } catch (InterruptedException e){
            e.printStackTrace();
        }
        finally {
            executor.shutdown();
        }
    }



    public static Double run_sequence(long from, long to, FuncType type) {
        double sum = 0;
        for (long i = from; i < to; i++) {
            double a = ((i % 2000) - 1000) / 33.0;
            double b = ((i % 200) - 100) / 22.0;
            double c = ((i % 20) - 10) / 11.0;
            sum += call_solver(type, a, b, c);
        }
        return sum;
    }
}
