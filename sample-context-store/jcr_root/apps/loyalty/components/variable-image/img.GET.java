/*
 * Copyright 1997-2008 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */

package apps.loyalty.components.variable_002dimage;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.servlet.http.HttpServletResponse;

import com.day.cq.wcm.foundation.Image;
import com.day.cq.wcm.commons.RequestHelper;
import com.day.cq.wcm.commons.WCMUtils;
import com.day.cq.wcm.commons.AbstractImageServlet;
import com.day.cq.commons.SlingRepositoryException;
import com.day.image.Layer;
import com.day.image.font.AbstractFont;
import com.day.image.Font;
import com.adobe.granite.security.user.UserProperties;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * Renders an image
 */
public class img_GET extends AbstractImageServlet {

    protected Layer createLayer(ImageContext c)
            throws RepositoryException, IOException {
        // don't create the later yet. handle everything later
        return null;
    }

    protected void writeLayer(SlingHttpServletRequest req,
                              SlingHttpServletResponse resp,
                              ImageContext c, Layer layer)
            throws IOException, RepositoryException {

        Image image = new Image(c.resource);
        image.setItemName(Image.NN_FILE, "image");
        image.setItemName(Image.PN_REFERENCE, "imageReference");
        if (!image.hasContent()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // get style and set constraints
        image.set(Image.PN_MIN_WIDTH, c.properties.get("minWidth", ""));
        image.set(Image.PN_MIN_HEIGHT, c.properties.get("minHeight", ""));
        image.set(Image.PN_MAX_WIDTH, c.properties.get("maxWidth", ""));
        image.set(Image.PN_MAX_HEIGHT, c.properties.get("maxHeight", ""));

        // get pure layer
        layer = image.getLayer(false, false, false);

        // crop
        boolean modified = image.crop(layer) != null;

        // resize
        modified |= image.resize(layer) != null;

        // rotate
        modified |= image.rotate(layer) != null;

        UserProperties u = req.getResourceResolver().adaptTo(UserProperties.class);
		String name = u.getProperty(UserProperties.DISPLAY_NAME);
        if (name == null) {
            name = u.getAuthorizableID();
        }
        String prefix = c.properties.get("prefix", "");
        String suffix = c.properties.get("suffix", "");

        Font font = new Font("helvetica", 24);
        layer.setPaint(java.awt.Color.WHITE);
        int x = c.properties.get("x", 0);
        int y = c.properties.get("y", 0);
        layer.drawText(x, y, 0, 0, prefix + name + suffix, font, AbstractFont.ALIGN_LEFT, 0, 0);
        modified = true;

        if (modified) {
            resp.setContentType(c.requestImageType);
            layer.write(c.requestImageType, 1.0, resp.getOutputStream());
        } else {
            // do not re-encode layer, just spool
            Property data = image.getData();
            InputStream in = data.getStream();
            resp.setContentLength((int) data.getLength());
            String contentType = image.getMimeType();
            if (contentType.equals("application/octet-stream")) {
                contentType=c.requestImageType;
            }
            resp.setContentType(contentType);
            IOUtils.copy(in, resp.getOutputStream());
            in.close();
        }
        resp.flushBuffer();
    }
}