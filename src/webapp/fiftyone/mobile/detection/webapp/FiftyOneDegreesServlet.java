package fiftyone.mobile.detection.webapp;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* *********************************************************************
 * This Source Code Form is copyright of 51Degrees Mobile Experts Limited. 
 * Copyright 2014 51Degrees Mobile Experts Limited, 5 Charlotte Close,
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
@SuppressWarnings("serial")
public final class FiftyOneDegreesServlet extends HttpServlet {

    final private static Logger logger = LoggerFactory
            .getLogger(FiftyOneDegreesServlet.class);
    private static final String JAVASCRIPT_CORE = "/core.js";
    private static final String JAVASCRIPT_FEATURES = "/features.js";
    private JavascriptProvider javascriptProvider;
    private ImageOptimizer imageResizer;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        javascriptProvider = new JavascriptProvider(getServletContext());
        imageResizer = new ImageOptimizer(config.getServletContext(),
                new Cache((File) getServletContext().getAttribute(
                ServletContext.TEMPDIR)));
        logger.info("init");
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {

        // Check the path information to find the resource being requested.
        String pathInfo = request.getPathInfo();
        if (JAVASCRIPT_CORE.equals(pathInfo)) {
            javascriptProvider.provide(request, response);
        } else if (JAVASCRIPT_FEATURES.equals(pathInfo)) {
            javascriptProvider.provideFeatures(request, response);
        } else if (pathInfo.endsWith("png")
                || pathInfo.endsWith("jpg")
                || pathInfo.endsWith("gif")) {
            imageResizer.image(request, response);
        }
    }
}