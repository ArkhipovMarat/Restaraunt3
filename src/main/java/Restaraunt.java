import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Restaraunt {

    private static final int MAXORDERS = 5;
    private static final int COOCKINGTIME = 200;
    private static final int AWAITINGTIME = 500;
    volatile private static int orders = 0;

    private static Lock lock = new ReentrantLock();
    private static Condition conditionOfficiant = lock.newCondition();
    private static Condition conditionKitchener = lock.newCondition();
    private static Condition conditionVisitor = lock.newCondition();
    volatile private static OrderStatus orderStatus = OrderStatus.NULL;

    public static void main(String[] args) throws InterruptedException {
        new Officiant("1").start();
        new Officiant("2").start();
        new Officiant("3").start();
        new Kitchener("1").start();
        Thread.sleep(AWAITINGTIME);
        System.out.println("---------------");

        for (int i = 1; i < 6; i++) {
            Thread.sleep(AWAITINGTIME);
            new Visitor(Integer.toString(i)).start();
        }
    }

    static class Visitor extends Thread {
        public Visitor(String name) {
            super(name);
        }

        @Override
        public void run() {
            String visitorName = Thread.currentThread().getName();
            System.out.println("Посетитель " + visitorName + ": в ресторане");

            lock.lock();
            orderStatus = OrderStatus.NOTIFY;
            conditionOfficiant.signalAll();
            lock.unlock();

            try {
                lock.lock();
                conditionVisitor.await();
                System.out.println("Посетитель " + visitorName + ": приступил к еде");
                lock.unlock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Посетитель " + visitorName + ": покинул ресторан");
        }
    }

    static class Officiant extends Thread {
        public Officiant(String name) {
            super(name);
        }

        @Override
        public void run() {
            String officiantName = Thread.currentThread().getName();
            System.out.println("Оффициант " + officiantName + ": в ресторане");

            while (orders <= MAXORDERS) {
                switch (orderStatus) {
                    case NOTIFY:
                        lock.lock();
                        orderStatus = OrderStatus.COCKING;
                        conditionKitchener.signal();
                        System.out.println("Оффициант " + officiantName + ": принял заказ");
                        lock.unlock();
                        break;
                    case READY:
                        lock.lock();
                        orderStatus = OrderStatus.NULL;
                        conditionVisitor.signal();
                        System.out.println("Оффициант " + officiantName + ": несет заказ посетителю");
                        lock.unlock();
                        break;
                    default:
                        try {
                            lock.lock();
                            conditionOfficiant.await();
                            lock.unlock();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                }
            }
        }
    }

    static class Kitchener extends Thread {
        public Kitchener(String name) {
            super(name);
        }

        @Override
        public void run() {
            System.out.println("повар: в ресторане");

            while (orders < MAXORDERS) {
                if (orderStatus.equals(OrderStatus.COCKING)) {
                    try {
                        lock.lock();
                        System.out.println("Повар: принял заказ и готовит еду");
                        Thread.sleep(COOCKINGTIME);
                        System.out.println("Повар: приготовил заказ");
                        orderStatus = OrderStatus.READY;
                        orders++;
                        conditionOfficiant.signal();
                        lock.unlock();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        lock.lock();
                        conditionKitchener.await();
                        lock.unlock();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }


}
