/*
 * This Source Code Form is copyright of 51Degrees Mobile Experts Limited.
 * Copyright © 2017 51Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY
 *
 * This Source Code Form is the subject of the following patents and patent
 * applications, owned by 51Degrees Mobile Experts Limited of 5 Charlotte
 * Close, Caversham, Reading, Berkshire, United Kingdom RG4 7BY:
 * European Patent No. 2871816;
 * European Patent Application No. 17184134.9;
 * United States Patent Nos. 9,332,086 and 9,350,823; and
 * United States Patent Application No. 15/686,066.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 */
package fiftyone.mobile.test.type.comparison.lite;

import fiftyone.mobile.Filename;
import fiftyone.mobile.detection.Provider;
import fiftyone.mobile.detection.factories.MemoryFactory;
import fiftyone.mobile.detection.factories.StreamFactory;
import fiftyone.mobile.TestType;
import fiftyone.mobile.test.type.comparison.ComparisonBase;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

@Category(TestType.TypeComparison.class)
public class LiteV32ComparisonTest extends ComparisonBase {
    
    private static Provider streamProvider;
    private static Provider memoryProvider;
    
    @BeforeClass
    public static void setUp() throws IOException {
        streamProvider = 
                new Provider(StreamFactory.create(Filename.LITE_PATTERN_V32, false));
        memoryProvider =
                new Provider(MemoryFactory.create(Filename.LITE_PATTERN_V32));
    }
    
    @AfterClass
    public static void tearDown() throws IOException {
        streamProvider.dataSet.close();
        memoryProvider.dataSet.close();
    }

    @Override
    public Provider getStreamProvider() {
        return streamProvider;
    }

    @Override
    public Provider getMemoryProvider() {
        return memoryProvider;
    }
}
