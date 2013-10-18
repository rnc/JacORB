package org.jacorb.test.bugs.bugjac330;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.jacorb.test.BasicServer;
import org.jacorb.test.BasicServerHelper;
import org.jacorb.test.common.CommonSetup;
import org.jacorb.test.common.ORBTestCase;
import org.jacorb.test.common.ServerSetup;
import org.jacorb.test.orb.BasicServerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.NO_RESOURCES;

/**
 * @author Alphonse Bendt
 */
public class MultipleServerTest extends ORBTestCase
{
    private ServerSetup setup1;
    private String server1IOR;
    private ServerSetup setup2;
    private String server2IOR;

    @Before
    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.put(CommonSetup.JACORB_REGRESSION_DISABLE_IMR, "true");
        props.put(CommonSetup.JACORB_REGRESSION_DISABLE_SECURITY, "true");

        setup1 = new ServerSetup(BasicServerImpl.class.getName());
        setup1.setUp();
        server1IOR = setup1.getServerIOR();

        setup2 = new ServerSetup(BasicServerImpl.class.getName());
        setup2.setUp();
        server2IOR = setup2.getServerIOR();
    }

    @After
    public void tearDown() throws Exception
    {
        setup2.tearDown();
        setup1.tearDown();
    }

    @Override
    protected void patchORBProperties(Properties props) throws Exception
    {
        props.put("jacorb.connection.client.max_receptor_threads", "1");

        if (name.getMethodName().equals("testNoIdleThreads"))
        {
            props.put("jacorb.connection.client.max_idle_receptor_threads", "0");
        }
    }

    @Test
    public void testAccessTwoServersAtOnceShouldFail() throws Exception
    {
        BasicServer server1 = BasicServerHelper.narrow(orb.string_to_object(server1IOR));
        assertEquals(10, server1.bounce_long(10));

        BasicServer server2 = BasicServerHelper.narrow(orb.string_to_object(server2IOR));

        try
        {
            server2.bounce_long(10);
            fail();
        }
        catch (NO_RESOURCES e)
        {
            // expected
        }
    }

    @Test
    public void testAccessTwoServersOneByOne() throws Exception
    {
        BasicServer server1 = BasicServerHelper.narrow(orb.string_to_object(server1IOR));
        assertEquals(10, server1.bounce_long(10));
        server1._release();

        // give the ConsumerReceptorThread some time to finish its work
        Thread.sleep(1000);

        BasicServer server2 = BasicServerHelper.narrow(orb.string_to_object(server2IOR));
        assertEquals(10, server2.bounce_long(10));
        server2._release();
    }

    @Test
    public void testAccessTwoServersAtOnceReleaseTryAgain() throws Exception
    {
        BasicServer server1 = BasicServerHelper.narrow(orb.string_to_object(server1IOR));
        assertEquals(10, server1.bounce_long(10));

        BasicServer server2 = BasicServerHelper.narrow(orb.string_to_object(server2IOR));

        try
        {
            server2.bounce_long(10);
            fail("should fail as there may not be more than 1 ClientReceptorThreads");
        }
        catch (NO_RESOURCES e)
        {
            // expected
        }

        server1._release();

        // give the ConsumerReceptorThread some time to finish its work
        Thread.sleep(1000);

        // retry bind
        assertEquals(10, server2.bounce_long(10));
    }

    @Test
    public void testNoIdleThreads() throws Exception
    {
        BasicServer server1 = BasicServerHelper.narrow(orb.string_to_object(server1IOR));
        assertEquals(10, server1.bounce_long(10));

        final String threadName = "ClientMessageReceptor";
        assertTrue(isThereAThreadNamed(threadName));

        server1._release();

        int retry = 0;
        final int maxRetry = 30;

        while( (retry++ < maxRetry) && isThereAThreadNamed(threadName))
        {
            // wait some time to allow the ClientMessageReceptor Thread to exit
            Thread.sleep(1000);
            System.gc();
        }

        dumpThread(threadName);

        assertFalse("there should be no idle thread", isThereAThreadNamed(threadName));
    }

    private void dumpThread(final String threadName) throws Exception
    {
        Method method;

        try
        {
            method = Thread.class.getMethod("getAllStackTraces", new Class[0]);
        }
        catch (NoSuchMethodException e)
        {
            // not a JDK 1.5
            return;
        }

        @SuppressWarnings("unchecked")
        Map<Thread, StackTraceElement[]> map = (Map<Thread, StackTraceElement[]>) method.invoke(null, new Object[0]);

        Iterator<Thread> i = map.keySet().iterator();

        while(i.hasNext())
        {
            Thread key = i.next();

            if (!key.getName().startsWith(threadName))
            {
                continue;
            }

            StackTraceElement[] stack = map.get(key);

            System.out.println(key.getName());
            for (int j = 0; j < stack.length; j++)
            {
                System.out.println("\t" + stack[j]);
            }
            System.out.println();
        }
    }

    private boolean isThereAThreadNamed(String name)
    {
        // begin hack.
        // fetch the names of all active threads and see if
        // the name matches.
        int threadCount = Thread.activeCount();
        Thread[] threads = new Thread[threadCount];

        Thread.enumerate(threads);

        for (int i = 0; i < threads.length; i++)
        {
            if (threads[i] == null)
            {
                continue;
            }

            if (threads[i].getName().indexOf(name) >= 0)
            {
                return true;
            }
        }
        return false;
    }
}
