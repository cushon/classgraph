/*
 * This file is part of ClassGraph.
 *
 * Author: Luke Hutchison
 *
 * Hosted at: https://github.com/classgraph/classgraph
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Luke Hutchison
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.classgraph.issues.issue875;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Issue875Test.
 */
public class Issue875Test {
    /**
     * The Class SuperSuperCls.
     */
    private static class SuperSuperCls {
    }

    /**
     * The Class SuperCls.
     */
    private static class SuperCls extends SuperSuperCls {
    }

    /**
     * The Class Cls.
     */
    private static class Cls extends SuperCls {
    }

    /**
     * Issue 875 test.
     */
    @Test
    public void issue875Test() throws IOException {

        Path path = Files.createTempFile("tmp", ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(path))) {
            Class<?> clazz = Cls.class;
            String name = clazz.getName().replace('.', '/') + ".class";
            jos.putNextEntry(new JarEntry(name));
            InputStream is = clazz.getClassLoader().getResourceAsStream(name);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) != -1) {
                jos.write(buffer, 0, length);
            }
        }
        URL url = path.toUri().toURL();
        ClassLoader loader = new URLClassLoader(new URL[] {url});

        // Accept only the class Cls, so that SuperCls and SuperSuperCls are external classes
        try (ScanResult scanResult =
                new ClassGraph()
                        .enableRealtimeLogging()
                        .addClassLoader(loader)
                        .acceptPackagesNonRecursive("io.github.classgraph.issues.issue875")
                        .ignoreParentClassLoaders()
                        .enableExternalClasses()
                        .enableAllInfo()
                        .scan()) {
            assertThat(scanResult.getSubclasses(SuperSuperCls.class).getNames())
                    .containsOnly(SuperCls.class.getName(), Cls.class.getName());
        }
    }
}
