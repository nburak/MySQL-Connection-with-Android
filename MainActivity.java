package concurrent.bk.concurrencyapplication;

import android.os.Build;
import android.os.Debug;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    public void ParallelStreamExample(View view)
    {
        // This process can be used for analyzing huge data sets with features of concurrency.
        StreamClass streamClass = new StreamClass();
        ArrayList<Integer> nums = new ArrayList<Integer>();
        for(int i = 0; i < 300000;i++)
        {
            nums.add(i);
        }
        Log.d("Sum of Evens",streamClass.SumOfEvens(nums)+"");
        Log.d("Sum of Odds",streamClass.SumOfOdds(nums)+"");
        Log.d("Sum of Alls",streamClass.SumOfAlls(nums)+"");
    }
    public void CountDownLatchExample(View view)
    {
        /*
            This is a concurrency process that help people want to do a process after all required process(made by several threads) completed.
            For example, in a huge social media application you can collect datas of user with many threads at the same time.
            And you should wait Until all of threads to complete their process.
        */

        CountDownLatch countDownLatch = new CountDownLatch(10);
        for(int i = 0; i < 10; i++)
        {
            TaskThread taskThread = new TaskThread(countDownLatch,i);
            taskThread.start();
        }
        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {

        }
        Log.d("Info","All of tasks were executed!");
    }
    public void ForkJoinExample(View view)
    {
        /*
            Fork Join Pool looks at size of data set. if its under base level, then it does process and returns result. Otherwise, it will share responsibilities
            To sub thread and return result after finishing.
        */
        ArrayList<User> users = new ArrayList<User>();
        for(int j = 0; j < 10000; j++)
        {
            User user = new User();
            user.setCredentials("User "+j,"123");
            users.add(user);
        }
        ForkJoinPool fjPool = new ForkJoinPool();
        User sampleUser = new User();
        sampleUser.setCredentials("User 145","123");
        User result = fjPool.invoke(new UserAuthenticator(0,users.size(),sampleUser,users));

        if(result != null)
        {
            Log.d("Result","User Found");
        }
        else
        {
            Log.d("Result","User Not Found");
        }
    }
    public void SemaphoreExample(View view)
    {
        // This can be used when you want threads to run with order.
        Semaphore semaphore = new Semaphore(1);
        ProcessMonitor processMonitor = new ProcessMonitor();
        for(int i = 0; i < 5 ; i++)
        {
            OrderedThread thread = new OrderedThread(semaphore,i,processMonitor);
            thread.start();
        }
    }
    public void ProducerConsumerExample(View view)
    {
        /*
            Sometime data of big application can be tried to edit by thread when it reads by other threads.
            This is a kind of template to manage reading and writing process.
        */
        ReadWriteMonitor readWriteMonitor = new ReadWriteMonitor();
        for(int i = 0; i < 40; i++)
        {
            Writer writer = new Writer(i,readWriteMonitor);
            writer.start();

            Reader reader = new Reader(i,readWriteMonitor);
            reader.start();
        }
    }
}

class User
{
    private String username,password;

    public void setCredentials(String username,String password)
    {
        this.username = username;
        this.password = password;
    }
    public String getUsername()
    {
        return username;
    }
    public String getPassword()
    {
        return password;
    }
}
class UserAuthenticator extends RecursiveTask<User>
{
    private int start,end;
    private User user;
    private ArrayList<User> users;
    public UserAuthenticator(int start,int end,User user,ArrayList<User> users)
    {
        this.start = start;
        this.end = end;
        this.user = user;
        this.users = users;
    }
    @Override
    protected User compute()
    {
        User matchedUser = null;
        if(end-start < 1000)
        {
            for(int i = 0; i < users.size();i++)
            {
                if(users.get(i).getUsername().equals(user.getUsername()) && users.get(i).getPassword().equals(user.getPassword()))
                {
                    matchedUser = users.get(i);
                }
            }
        }
        else
        {
            int mid = start + (end - start) / 2;
            UserAuthenticator left = new UserAuthenticator(start, mid, user,users);
            UserAuthenticator right = new UserAuthenticator(mid, end, user,users);
            left.fork();
            right.fork();
            User rUser = right.join();
            User lUser = left.join();

            if(rUser != null)
            {
                matchedUser = rUser;
            }
            else if(lUser != null)
            {
                matchedUser = lUser;
            }
            return matchedUser;
        }
        return matchedUser;
    }
}

class ReadWriteMonitor
{
    private Lock lock = new ReentrantLock();
    private Condition writing = lock.newCondition();
    private Condition reading = lock.newCondition();
    private Boolean isWriting,isReading,shouldRead;
    private int numberOfReaders,numberOfWriters;
    private ArrayList<Integer> list;
    public ReadWriteMonitor()
    {
        isWriting = true;
        isReading = false;
        shouldRead = false;
        numberOfReaders = 0;
        numberOfWriters = 0;

        list = new ArrayList<Integer>();
    }
    public void startWriting()
    {
        if(numberOfWriters > 0 || numberOfReaders > 0)
        {
            try
            {
                writing.signal();
                writing.await();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            isWriting = true;
        }
    }
    public void stopWriting()
    {
        try
        {
            lock.lock();
            isWriting = false;
            numberOfWriters = 0;
            if(list.size()%5 == 0 && list.size()!=0)
            {
                shouldRead = true;
                reading.signal();
            }
            else
            {
                writing.signal();
            }

        }
        catch (Exception e)
        {

        }
        finally {
            lock.unlock();
        }
    }
    public void write(int id)
    {
        list.add(id);
        Log.d("Added",id+"");
    }
    public void startReading()
    {
        lock.lock();
        try
        {
            while (isWriting || shouldRead == false)
            {
                reading.await();
            }
            numberOfReaders++;
        }
        catch (Exception e)
        {

        }
        finally
        {
            lock.unlock();
        }
    }
    public void read(int id)
    {
        Log.d("Readed",list.get(list.size()-1)+" readed by Reader "+id);
    }
    public void stopReading()
    {
        lock.lock();
        try
        {
            numberOfReaders--;
            if(numberOfReaders == 0)
            {
                shouldRead = false;

                writing.signal();
            }
        }
        catch (Exception e)
        {

        }
        finally {
            lock.unlock();
        }
    }
}

class ProcessMonitor
{
    private Lock lock = new ReentrantLock();

    public void DoProcess(String id)
    {
        try
        {
            lock.lock();
            Log.d("Thread",id);
        }
        catch (Exception e)
        {

        }
        finally
        {
            lock.unlock();
        }
    }
}
class OrderedThread extends Thread
{
    private Semaphore semaphore;
    private int id;
    private ProcessMonitor processMonitor;
    public OrderedThread(Semaphore semaphore,int id,ProcessMonitor processMonitor)
    {
        try
        {
            Thread.sleep(10);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        this.id = id;
        this.semaphore = semaphore;
        this.processMonitor = processMonitor;
    }
    public void run()
    {
        try
        {
            semaphore.acquire();
            processMonitor.DoProcess(id+"");
            semaphore.release();
        }
        catch (Exception e)
        {

        }
    }
}

class Writer extends Thread
{
    int id;ReadWriteMonitor readWriteMonitor;
    public Writer(int id,ReadWriteMonitor readWriteMonitor)
    {
        this.id = id;
        this.readWriteMonitor = readWriteMonitor;
    }
    public void run()
    {
        readWriteMonitor.startWriting();
        readWriteMonitor.write(id);
        readWriteMonitor.stopWriting();
    }
}

class Reader extends Thread
{
    int id;ReadWriteMonitor readWriteMonitor;
    public Reader(int id,ReadWriteMonitor readWriteMonitor)
    {
        this.id = id;
        this.readWriteMonitor = readWriteMonitor;
    }
    public void run()
    {
        readWriteMonitor.startReading();
        readWriteMonitor.read(id);
        readWriteMonitor.stopReading();
    }
}

class TaskThread extends Thread
{
    CountDownLatch countDownLatch;
    int id;
    public TaskThread(CountDownLatch countDownLatch,int id)
    {
        this.countDownLatch = countDownLatch;
        this.id = id;
    }
    public void run()
    {
        Log.d("Executed","Thread "+id);
        countDownLatch.countDown();
    }
}

class StreamClass
{
    public int SumOfEvens(ArrayList<Integer> nums)
    {
        return (int) nums.parallelStream().reduce(0,(sum,x) -> x % 2 == 0 ? sum + x : sum);
    }
    public int SumOfOdds(ArrayList<Integer> nums)
    {
        return (int) nums.parallelStream().filter(x -> x % 2 != 0).reduce(0,(sum,x) -> x % 1 == 0 ? sum + x : sum);
    }
    public int SumOfAlls(ArrayList<Integer> nums)
    {
        return (int) nums.parallelStream().reduce(0,(sum,x) -> x % 1 == 0 ? sum + x : sum);
    }
}