package org.jacorb.test.common;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2012 Gerald Brose / The JacORB Team.
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.AssertionFailedError;
import org.junit.Assert;

/**
 * Utility class used to setup JUnit-TestSuite
 *
 * @author Alphonse Bendt
 */

public class TestUtils
{
    public static final boolean isIBM = (System.getProperty ("java.vendor").equals ("IBM Corporation"));

    private static String testHome = null;
    private static String systemRoot = null;

    static boolean verbose = "true".equalsIgnoreCase(System.getProperty("jacorb.test.verbose"));


    public static String jacorbHome()
    {
        return System.getProperty("jacorb.home");
    }

    /**
     * Returns the name of the home directory of this regression suite.
     */
    public static String testHome()
    {
        if (testHome == null)
        {
            //See if we set it as a property using TestLauncher
            testHome = System.getProperty ("jacorb.test.home");

            //Try to find it from the run directory
            if (testHome == null)
            {
                URL url = TestUtils.class.getResource("/.");

                String result = url.toString();
                if (result.matches("file:/.*?"))
                {
                    result = result.substring (5, result.length() - 9);
                }
                String relativePath="/classes/";
                if (!pathExists(result, relativePath))
                {
                    throw new RuntimeException ("cannot find test home");
                }
                testHome = osDependentPath(result);
            }
        }

        if (testHome == null)
        {
            throw new RuntimeException("unable to determine testhome (set it with -Djacorb.test.home)");
        }

        return testHome;
    }

    public static String osDependentPath(String path)
    {
        path=(new File(path)).toString();
        return path.trim();
    }

    private static boolean pathExists(String basePath, String suffix)
    {
        File filePath = new File(basePath + suffix);
        return filePath.exists();
    }

    /**
     * In addition to file and path separators being different,
     * Windows requires an additional environment variable for
     * SystemRoot in DirectLauncer.
     */
    public static boolean isWindows()
    {
        return (System.getProperty("os.name").indexOf("Windows") != -1) ;
    }

    /**
     * Returns the SystemRoot, should be used on Windows only.  This
     * is necessary to prevent the following error:
     * "Unrecognised Windows Sockets error: 10106: create"
     */
    public static String systemRoot() throws RuntimeException, java.io.IOException
    {

        if (isWindows())
        {
            if (systemRoot == null)
            {
                //See if we set it as a property using TestLauncher.
                //This means we are likely in the DirectLauncher
                systemRoot = System.getProperty ("jacorb.SystemRoot");

                if (systemRoot == null)
                {
                    //We are likely in the TestLauncher, see if we can
                    //get it from the system environment.
                    Properties env = new Properties();
                    try
                    {
                        String setCmd = getSetCommand();
                        Process proc = Runtime.getRuntime().exec(setCmd);
                        InputStream ioStream = proc.getInputStream();
                        env.load(ioStream);
                        systemRoot = env.getProperty("SystemRoot");
                        ioStream.close();
                        proc.destroy();
                    }
                    catch(java.io.IOException e)
                    {
                        throw e;
                    }
                    if (systemRoot == null)
                    {
                        throw new RuntimeException("Could not find SystemRoot, make sure SystemRoot env var is set");
                    }
                    //The '\' character gets interpeted as an escape
                    if (systemRoot.charAt(1) == ':' && systemRoot.charAt(2) != '\\')
                    {
                        String prefix = systemRoot.substring(0, 2);
                        String suffix = systemRoot.substring(2, systemRoot.length());
                        systemRoot = prefix + "\\" + suffix;
                    }
                }
            }
        }
        else
        {
            throw new RuntimeException("TestUtils.systemRoot() was called on a non-Windows OS");
        }
        return systemRoot;
    }

    /**
     * Private method to get the set command.  This is necessary because
     * if SystemRoot has not been set, it needs to be set for DirectLauncer.
     */
    private static String getSetCommand()
    {
        String setCmd;
        String osName = System.getProperty("os.name");

        if (osName.indexOf("indows")  != -1)
        {
            if (osName.indexOf("indows 9") != -1)
            {
                setCmd = "command.com /c set";
            }
            else
            {
                setCmd = "cmd.exe /c set";
            }
        }
        else
        {
            setCmd = "/usr/bin/env";
            //should double check for all unix platforms
        }
        return setCmd;
    }

    public static void log(String string)
    {
        if (verbose)
        {
            System.err.println (string);
        }
    }

    /**
     * create properties that contain the correct (JDK specific)
     * settings to create a Sun or IBM ORB.
     */
    public static Properties newForeignORBProperties()
    {
        final Properties props = new Properties();

        if (TestUtils.isIBM)
        {
            props.setProperty ("org.omg.CORBA.ORBClass", "com.ibm.CORBA.iiop.ORB");
            props.setProperty ("org.omg.CORBA.ORBSingletonClass", "com.ibm.rmi.corba.ORBSingleton");
        }
        else
        {
            props.setProperty("org.omg.CORBA.ORBClass", "com.sun.corba.se.impl.orb.ORBImpl");
            props.setProperty("org.omg.CORBA.ORBSingletonClass", "com.sun.corba.se.impl.orb.ORBSingleton");
        }
        return props;
    }

    public static boolean getStringAsBoolean(String value)
    {
        return "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    public static void deleteRecursively(File name)
    {
        if (name.isDirectory())
        {
            File[] subdirs = name.listFiles(new FileFilter()
            {
                public boolean accept(File pathname)
                {
                    return pathname.isDirectory();
                }
            });

            for (int i = 0; i < subdirs.length; ++i)
            {
                deleteRecursively(subdirs[i]);
            }

            File[] files = name.listFiles(new FileFilter()
            {
                public boolean accept(File pathname)
                {
                    return pathname.isFile();
                }
            });

            for (int i = 0; i < files.length; ++i)
            {
                files[i].delete();
            }

            name.delete();
        }

        if (name.isFile())
        {
            name.delete();
        }
    }

    public static File createTempDir(final String name)
    {
        String dir = System.getProperty("java.io.tmpdir");
        dir += File.separator + name + "-" + System.currentTimeMillis();
        final File tmpDir = new File(dir);
        Assert.assertTrue(tmpDir.mkdir());
        tmpDir.deleteOnExit();
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                TestUtils.deleteRecursively(tmpDir);
            };
        });

        return tmpDir;
    }

    private static File[] getSubDirectories(File src)
    {
        return src.listFiles(new FileFilter()
        {
            public boolean accept(File pathname)
            {
                return pathname.isDirectory();
            }
        });
    }

    private static List<File> getFilesInDirectory(File src, final String suffix)
    {
        final File[] fileList = src.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(suffix);
            }
        });
        Assert.assertNotNull(src + " does not exist.", fileList);
        return Arrays.asList(fileList);
    }

    public static List<File> getJavaFilesRecursively(File src)
    {
        final String suffix = ".java";
        return getFilesRecursively(src, suffix);
    }

    public static List<File> getFilesRecursively(File src, final String suffix)
    {
        List<File> result = new ArrayList<File>();
        result.addAll(getFilesInDirectory(src, suffix));

        File[] dirs = getSubDirectories(src);

        for (int i = 0; i < dirs.length; ++i)
        {
            result.addAll(getFilesRecursively(dirs[i], suffix));
        }

        return result;
    }

    public static ClassLoader compileJavaFiles(File dirCompilation, File[] files, boolean failureExpected) throws IOException
    {
        Assert.assertNotNull(files);

        if (files.length == 0)
        {
            return null;
        }

        dirCompilation.mkdir();

        Assert.assertTrue(dirCompilation.isDirectory());
        Assert.assertTrue(dirCompilation.exists());
        Assert.assertTrue(dirCompilation.canWrite());
        File file = new File(dirCompilation, "files.txt");
        file.delete();
        file.createNewFile();

        PrintWriter writer = new PrintWriter(new FileWriter(file));

        for (int i = 0; i < files.length; ++i)
        {
            writer.println(files[i].getAbsolutePath());
        }

        writer.close();

        String javaHome = System.getProperty("java.home");
        String testHome = testHome();
        String classpath = testHome + File.separator + ".." + File.separator + ".." + File.separator + "classes";

        if (javaHome.endsWith("jre"))
        {
            javaHome = javaHome.substring(0, javaHome.length() - 4);
        }
        String cmd = javaHome + "/bin/javac -d " + dirCompilation + " -bootclasspath " + classpath + ":" + System.getProperty("sun.boot.class.path") + " @" + file.getAbsolutePath();

        TestUtils.log("[COMPILE] " + cmd);
        TestUtils.log("[COMPILE] " + files.length + " java files");
        try
        {
            Process proc = Runtime.getRuntime().exec(cmd);

            int exit = proc.waitFor();
            if (failureExpected && exit == 0)
            {
                Assert.fail("should fail: " + cmd);
            }

            if (exit != 0)
            {
                InputStream in = proc.getErrorStream();
                LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));
                StringBuffer b = new StringBuffer();

                String line;
                while ((line = reader.readLine()) != null)
                {
                    b.append(line);
                    b.append("\n");
                }

                Assert.fail(cmd + "\n" + b.toString());
            }

            return new URLClassLoader(new URL[] {dirCompilation.toURI().toURL()});
        }
        catch (Exception e)
        {
            if (!failureExpected)
            {
                AssertionFailedError error = new AssertionFailedError("cmd: " + cmd);
                error.initCause(e);
                throw error;
            }
            return null;
        }
    }

    public static long getSystemPropertyAsLong(final String property, final long defaultValue)
    {
        long parseLong;
        try
        {
            parseLong = Long.parseLong(System.getProperty(property));
        }
        catch (Exception e)
        {
            parseLong = defaultValue;
        }
        return parseLong;
    }

    public static boolean getSystemPropertyAsBoolean(final String property, final boolean defaultValue)
    {
        String value = System.getProperty (property);
        if (value == null)
        {
            return defaultValue;
        }
        return getStringAsBoolean(value);
    }

    public static long getShortTimeout()
    {
        return Long.getLong("jacorb.test.timeout.short", 2000).longValue();
    }

    public static long getMediumTimeout()
    {
        return Long.getLong("jacorb.test.timeout.medium", 20000).longValue();
    }

    /**
     * wait until a server IOR shows up (file size > 0) and print it out
     * to stdout so it can be picked up by a client process.
     */
    public static void printServerIOR(File iorFile)
                                                    throws InterruptedException,
                                                    FileNotFoundException,
                                                    IOException
    {
        long maxWait = System.currentTimeMillis() + ServerSetup.getTestServerTimeout();

        while(iorFile.length() == 0 && System.currentTimeMillis() < maxWait)
        {
            Thread.sleep(1000);
        }

        Thread.sleep(1000);

        BufferedReader in = new BufferedReader(new FileReader(iorFile));
        String ior = in.readLine();
        in.close();

        if (ior == null)
        {
            throw new IllegalArgumentException("cannot read IOR from file " + iorFile + " within " + ServerSetup.getTestServerTimeout());
        }

        System.out.println("SERVER IOR: " + ior);
    }

    /**
     * copied here from ObjectUtil to make the package org.jacorb.test.common independent from the orb core
     */
    public static Class<?> classForName(String name)
        throws ClassNotFoundException, IllegalArgumentException
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Class name must not be null!");
        }

        try
        {
            // Here we prefer classLoader.loadClass() over the three-argument
            // form of Class.forName(), as the latter is reported to cause
            // caching of stale Class instances (due to a buggy cache of
            // loaded classes).
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        }
        catch (Exception e)
        {
            // As a fallback, we prefer Class.forName(name) because it loads
            // array classes (i.e., it handles arguments like
            // "[Lsome.class.Name;" or "[[I;", which classLoader.loadClass()
            // does not handle).
            return Class.forName(name);
        }
    }

    public static int patternMatcher(Pattern pattern, String text)
    {
        Matcher matcher = pattern.matcher(text);

        if (matcher.find())
        {
            return matcher.end();
        }

        return 0;
    }

}
