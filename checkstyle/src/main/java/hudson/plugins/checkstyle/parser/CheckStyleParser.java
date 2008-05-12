package hudson.plugins.checkstyle.parser;

import hudson.plugins.checkstyle.util.AnnotationParser;
import hudson.plugins.checkstyle.util.model.MavenModule;
import hudson.plugins.checkstyle.util.model.Priority;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

/**
 * A parser for Checkstyle XML files.
 *
 * @author Ulli Hafner
 */
public class CheckStyleParser implements AnnotationParser {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = -8705621875291182458L;

    /** {@inheritDoc} */
    public MavenModule parse(final InputStream file, final String moduleName) throws InvocationTargetException {
        try {
            Digester digester = new Digester();
            digester.setValidating(false);
            digester.setClassLoader(CheckStyleParser.class.getClassLoader());

            String rootXPath = "checkstyle";
            digester.addObjectCreate(rootXPath, CheckStyle.class);
            digester.addSetProperties(rootXPath);

            String fileXPath = "checkstyle/file";
            digester.addObjectCreate(fileXPath, hudson.plugins.checkstyle.parser.File.class);
            digester.addSetProperties(fileXPath);
            digester.addSetNext(fileXPath, "addFile", hudson.plugins.checkstyle.parser.File.class.getName());

            String bugXPath = "checkstyle/file/error";
            digester.addObjectCreate(bugXPath, Error.class);
            digester.addSetProperties(bugXPath);
            digester.addSetNext(bugXPath, "addError", Error.class.getName());

            CheckStyle module;
            module = (CheckStyle)digester.parse(file);
            if (module == null) {
                throw new SAXException("Input stream is not a Checkstyle file.");
            }

            return convert(module, moduleName);
        }
        catch (IOException exception) {
            throw new InvocationTargetException(exception);
        }
        catch (SAXException exception) {
            throw new InvocationTargetException(exception);
        }
    }

    /**
     * Converts the internal structure to the annotations API.
     *
     * @param collection
     *            the internal maven module
     * @param moduleName
     *            name of the maven module
     * @return a maven module of the annotations API
     */
    private MavenModule convert(final CheckStyle collection, final String moduleName) {
        MavenModule module = new MavenModule(moduleName);
        for (hudson.plugins.checkstyle.parser.File file : collection.getFiles()) {
            for (Error warning : file.getErrors()) {
                Priority priority;
                if ("error".equalsIgnoreCase(warning.getSeverity())) {
                    priority = Priority.HIGH;
                }
                else if ("warning".equalsIgnoreCase(warning.getSeverity())) {
                    priority = Priority.NORMAL;
                }
                else if ("info".equalsIgnoreCase(warning.getSeverity())) {
                    priority = Priority.LOW;
                }
                else {
                    continue; // ignore
                }
                String source = warning.getSource();
                String type = StringUtils.substringAfterLast(source, ".");
                String category = StringUtils.substringAfterLast(StringUtils.substringBeforeLast(source, "."), ".");
                Warning bug = new Warning(priority, warning.getMessage(), StringUtils.capitalize(category), type, warning.getLine());
                bug.setModuleName(moduleName);
                bug.setFileName(file.getName());

                module.addAnnotation(bug);
            }
        }
        return module;
    }

    /** {@inheritDoc} */
    public String getName() {
        return "CHECKSTYLE";
    }
}

