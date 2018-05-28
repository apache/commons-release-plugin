package org.apache.commons.release.plugin.velocity;

import static junit.framework.TestCase.assertTrue;

import java.io.StringWriter;
import java.io.Writer;
import org.junit.Test;

/**
 * Unit tests for {@link HeaderHtmlVelocityDelegate}
 */
public class HeaderHtmlVelocityDelegateTest {

    @Test
    public void testSuccess() {
        HeaderHtmlVelocityDelegate subject = HeaderHtmlVelocityDelegate.builder().build();
        Writer writer = new StringWriter();
        writer = subject.render(writer);
        assertTrue(writer.toString().contains("<h2>Apache Commons Project Distributions</h2>"));
    }

}
