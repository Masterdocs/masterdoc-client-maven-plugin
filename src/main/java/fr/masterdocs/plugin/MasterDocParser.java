package fr.masterdocs.plugin;

import static com.sun.codemodel.ClassType.ENUM;
import static com.sun.codemodel.JExpr._this;
import static com.sun.codemodel.JExpr.ref;
import static com.sun.codemodel.JMod.PRIVATE;
import static com.sun.codemodel.JMod.PUBLIC;
import static java.lang.Void.TYPE;
import static org.apache.commons.lang.StringUtils.capitalize;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.*;

import fr.masterdocs.pojo.AbstractEntity;
import fr.masterdocs.pojo.Entity;
import fr.masterdocs.pojo.Enumeration;
import fr.masterdocs.pojo.MasterDoc;

/**
 * User: pleresteux
 */
public class MasterDocParser {

  /** Logger. */
  private static Logger LOGGER              = LoggerFactory.getLogger(MasterDocParser.class);
  private ClassLoader   originalClassLoader = Thread.currentThread().getContextClassLoader();
  private ClassLoader   newClassLoader;
  private MavenProject  project;

  JCodeModel            codeModel;

  public MasterDocParser(MavenProject project) {
    this.project = project;
    codeModel = new JCodeModel();
  }

  public MasterDoc parseMasterdocFile(String fullFilePath) throws IOException {
    LOGGER.info("Parsing file {}...", fullFilePath);
    File fileToParse = new File(fullFilePath);
    if (!fileToParse.exists()) {
      throw new IllegalArgumentException(fullFilePath + " does not exist !");
    }
    return parseMasterdocFile(fileToParse);
  }

  public MasterDoc parseMasterdocFile(File fileToParse) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    MasterDoc masterdoc = mapper.readValue(fileToParse, MasterDoc.class);
    LOGGER.info("Parsing file reads Masterdoc {}", masterdoc);
    return masterdoc;
  }

  public void createEntities(MasterDoc masterdoc) throws JClassAlreadyExistsException, IOException, ClassNotFoundException, NoSuchFieldException,
      IllegalAccessException {
    this.createEntities(masterdoc.getEntities());
  }

  public void createEntities(List<AbstractEntity> entities) throws JClassAlreadyExistsException, IOException, ClassNotFoundException, NoSuchFieldException,
      IllegalAccessException {
    final ArrayList<AbstractEntity> errorEntityList = new ArrayList<AbstractEntity>();
    for (AbstractEntity entity : entities) {
      if (entity instanceof Enumeration) {
        createEnumeration((Enumeration) entity, errorEntityList);
      } else {
        createClass((Entity) entity, errorEntityList);
      }
    }
    generateProjectClassLoader(this.project);
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    URL url = classLoader.getResource(".");
    String path = getSourceDir(url);

    codeModel.build(new File(path));
    codeModel = new JCodeModel();
    if (entities.size() > errorEntityList.size()) {
      createEntities(errorEntityList);
    }
  }

  private String getSourceDir(URL url) {
    String path = url.getPath();
    if (path.endsWith("target/classes/")) {
      path = path.substring(0, path.length() - 15) + "src/main/java";
    }
    return path;
  }

  public void createEnumeration(Enumeration entity, ArrayList<AbstractEntity> errorEntityList) throws JClassAlreadyExistsException, IOException {

    JDefinedClass definedClass = codeModel._class(PUBLIC, entity.getName(), ENUM);
    final List<String> values = entity.getValues();
    for (String value : values) {
      definedClass.enumConstant(value);
    }
  }

  public void createClass(Entity entity, ArrayList<AbstractEntity> errorEntityList) throws JClassAlreadyExistsException, IOException {
    try {
      JDefinedClass definedClass = codeModel._class(entity.getName());
      final Map<String, AbstractEntity> fields = entity.getFields();
      if (null != fields && null != fields.keySet()) {
        final Iterator<String> iterator = fields.keySet().iterator();
        while (iterator.hasNext()) {
          String key = iterator.next();
          AbstractEntity field = fields.get(key);
          if (!field.isEnumeration()) {

            final Class<?> type;

            type = Class.forName(field.getName());

            final JFieldVar jfield = definedClass.field(PRIVATE, type, key);
            // Create the getter method and return the JFieldVar previously defined
            String getterMethodName = "get" + capitalize(key);
            JMethod getterMethod = definedClass.method(PUBLIC, type, getterMethodName);
            JBlock block = getterMethod.body();
            block._return(jfield);
            // Create the setter method and set the JFieldVar previously defined with the given parameter
            String setterMethodName = "set" + capitalize(key);
            JMethod setterMethod = definedClass.method(PUBLIC, TYPE, setterMethodName);
            String setterParameter = key;
            setterMethod.param(type, setterParameter);
            setterMethod.body().assign(_this().ref(key), ref(setterParameter));
          }
        }
      }
    } catch (ClassNotFoundException e) {
      errorEntityList.add(entity);
    }
  }

  /**
   * @param project
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  private void generateProjectClassLoader(MavenProject project) throws NoSuchFieldException, IllegalAccessException {
    List<URL> urls = new ArrayList<URL>();

    // get all the dependencies which are hidden in resolvedArtifacts of project object
    Field dependencies = MavenProject.class.
        getDeclaredField("resolvedArtifacts");

    dependencies.setAccessible(true);

    LinkedHashSet<Artifact> artifacts = (LinkedHashSet<Artifact>) dependencies.get(project);

    // noinspection unchecked
    for (Artifact artifact : artifacts) {
      try {
        urls.add(artifact.getFile().toURI().toURL());
      } catch (MalformedURLException e) {
        // logger.error(e);
      }
    }

    try {
      urls.add(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
    } catch (MalformedURLException e) {
      LOGGER.error(e.getMessage());
    }

    LOGGER.debug("urls = \n" + urls.toString().replace(",", "\n"));

    newClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), originalClassLoader);
    Thread.currentThread().setContextClassLoader(newClassLoader);
  }
}
