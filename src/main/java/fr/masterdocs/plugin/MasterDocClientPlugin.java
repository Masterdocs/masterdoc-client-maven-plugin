package fr.masterdocs.plugin;

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.sun.codemodel.JClassAlreadyExistsException;
import fr.masterdocs.pojo.MasterDoc;

/**
 * User: pleresteux
 */
@Mojo(name = "masterdoc-client", requiresDependencyResolution = COMPILE)
public class MasterDocClientPlugin extends AbstractMojo {

  @Parameter(property = "masterdocsFilePath", readonly = true, required = true)
  private String       masterdocsFilePath;

  /**
   * The Maven Project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  public void execute() throws MojoExecutionException {
    MasterDocParser masterDocParser = new MasterDocParser(project);
    try {
      MasterDoc masterdoc = masterDocParser.parseMasterdocFile(masterdocsFilePath);
      masterDocParser.createEntities(masterdoc);
    } catch (IOException e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    } catch (ClassNotFoundException e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    } catch (JClassAlreadyExistsException e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    } catch (IllegalAccessException e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    } catch (NoSuchFieldException e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    }
  }
}
