/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.bitcoin.core.utils;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * A Java logging formatter that writes more compact output than the default.
 */
public class BriefLogFormatter extends Formatter {
    private static final MessageFormat messageFormat = new MessageFormat("{3,date,hh:mm:ss} {0} {1}.{2}: {4}\n");

    /** Configures JDK logging to use this class for everything. */
    public static void init() {
        LogManager.getLogManager().getLogger("").getHandlers()[0].setFormatter(new BriefLogFormatter());
    }

    @Override
    public String format(LogRecord logRecord) {
        Object[] arguments = new Object[5];
        arguments[0] = logRecord.getThreadID();
        String fullClassName = logRecord.getSourceClassName();
        int lastDot = fullClassName.lastIndexOf('.');
        String className = fullClassName.substring(lastDot + 1);
        arguments[1] = className;
        arguments[2] = logRecord.getSourceMethodName();
        arguments[3] = new Date(logRecord.getMillis());
        arguments[4] = logRecord.getMessage();
        return messageFormat.format(arguments);
    }
}
