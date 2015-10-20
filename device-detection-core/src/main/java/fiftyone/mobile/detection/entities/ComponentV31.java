/* *********************************************************************
 * This Source Code Form is copyright of 51Degrees Mobile Experts Limited. 
 * Copyright © 2015 51Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY
 * 
 * This Source Code Form is the subject of the following patent 
 * applications, owned by 51Degrees Mobile Experts Limited of 5 Charlotte
 * Close, Caversham, Reading, Berkshire, United Kingdom RG4 7BY: 
 * European Patent Application No. 13192291.6; and 
 * United States Patent Application Nos. 14/085,223 and 14/085,301.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.
 * 
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 * ********************************************************************* */
package fiftyone.mobile.detection.entities;

import fiftyone.mobile.detection.Dataset;
import fiftyone.mobile.detection.readers.BinaryReader;
import fiftyone.properties.DetectionConstants;
import java.io.IOException;

/**
 * Returns constants for HTTP Headers.
 */
public class ComponentV31 extends Component {
    /**
     * List of HTTP headers that should be checked in order to perform a 
     * detection where more headers than User-Agent are available.
     */
    private volatile String[] httpHeaders;

    /**
     * Constructs a new instance of ComponentV31. Reads the string offsets to 
     * the HTTP Headers during the constructor.
     * @param dataSet The Dataset being created.
     * @param index Index of the component within the list.
     * @param reader Reader connected to the source data structure and 
     * positioned to start reading.
     */
    public ComponentV31(Dataset dataSet, int index, BinaryReader reader) {
        super(dataSet, index, reader);
        this.httpHeaders = null;
    }

    /**
     * List of HTTP headers that should be checked in order to perform a 
     * detection where more headers than User-Agent are available. This data 
     * is used by methods that can HTTP Header collections.
     * @return List of HTTP headers as Strings.
     * @throws java.io.IOException
     */
    @Override
    public String[] getHttpheaders() throws IOException {
        String[] localHttpHeaders = httpHeaders;
        if (localHttpHeaders == null) {
            synchronized(this) {
                localHttpHeaders = httpHeaders;
                if (localHttpHeaders == null) {
                    // Implemented as "if else" for 1.6 compatibility.
                    String cName = super.getName(); 
                    if (cName.equals("HardwarePlatform")) {
                        httpHeaders = localHttpHeaders =
                                DetectionConstants.DEVICE_USER_AGENT_HEADERS;
                    } else if (cName.equals("SoftwarePlatform")) {
                        httpHeaders = localHttpHeaders =
                                DetectionConstants.DEVICE_USER_AGENT_HEADERS;
                    } else if (cName.equals("BrowserUA")) {
                        httpHeaders = localHttpHeaders =
                                new String[] {
                                    DetectionConstants.USER_AGENT_HEADER
                                };
                    } else if (cName.equals("Crawler")) {
                        httpHeaders = localHttpHeaders =
                                new String[] {
                                    DetectionConstants.USER_AGENT_HEADER
                                };
                    }
                }
            }
        }
        return localHttpHeaders;
    }
    
    
    
}
