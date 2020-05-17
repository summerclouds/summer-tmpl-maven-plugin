package de.mhus.mvn.tmpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import de.mhus.lib.core.MFile;
import de.mhus.lib.core.MString;
import de.mhus.lib.core.logging.Log;

@Mojo(
        name = "tmpl",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        inheritByDefault = true,
        threadSafe = true)
public class TmplMojo extends AbstractMojo {

	private Log log = new MavenPluginLog(this);

	@Parameter
	public String filePrefix = "";
	@Parameter
	public String fileSuffix = "-tmpl";
	
	/**
     * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents
     * is being parsed.
     */
    @Parameter(required=true)
    private FileSet files;

    /**
     * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents
     * is being parsed.
     */
    @Parameter
    private String[] excludes = new String[] {};
	
	@Parameter(defaultValue = "${project}")
    protected MavenProject project;

	HashMap<String, Object> parameters = new HashMap<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
    	
		project.getProperties().forEach((k,v) -> putParameter(k,v));
		putParameter("project.version", project.getVersion());
		putParameter("project.groupId", project.getGroupId());
		putParameter("project.artifact", project.getArtifactId());
		putParameter("project.name", project.getName());
		putParameter("basedir", project.getBasedir());
		if (project.getParent() != null) {
			putParameter("parent.version", project.getParent().getVersion());
			putParameter("parent.groupId", project.getParent().getGroupId());
			putParameter("parent.artifact", project.getParent().getArtifactId());
		}

    	List<File> list = toFileList(files);
        
    	for (File file : list) {
    		if (file.getPath().contains("/.")) continue;
    		log.i("scan",file);
    		String name = MFile.getFileNameOnly(file.getName());
    		if (name.startsWith(filePrefix) && name.endsWith(fileSuffix))
    			tmplFile(file,name.substring(filePrefix.length(), name.length() - fileSuffix.length()) + "." + MFile.getFileExtension(file));
    	}
    	
    }

	private void putParameter(Object k, Object v) {
		String key = String.valueOf(k);
		Map<String, Object> cont = parameters;
		if (key.contains(".")) {
			String path = MString.beforeLastIndex(key, '.');
			cont = getParamContainer(cont, path);
			key = MString.afterLastIndex(key, '.');
		}
		cont.put(key, v);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getParamContainer(Map<String, Object> parent, String key) {
		Map<String, Object> cont = null;
		if (key.contains(".")) {
			String path = MString.beforeLastIndex(key, '.');
			cont = getParamContainer(parent, path);
			key = MString.afterLastIndex(key, '.');
		} else {
			cont = (Map<String, Object>) parent.get(key);
			if (cont == null) {
				cont = new HashMap<>();
				parent.put(key, cont);
			}
		}
		
		return cont;
	}

	private void tmplFile(File from, String to) {
		try {
			log.i("Tmpl",from,to);
			JtwigTemplate jtwigTemplate = JtwigTemplate.fileTemplate(from);
			
			JtwigModel jtwigModel = JtwigModel.newModel(parameters);
			
            FileOutputStream fos = new FileOutputStream(to);
				jtwigTemplate.render(jtwigModel, fos);
            fos.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public List<File> toFileList( FileSet fs ) throws MojoExecutionException
	{
	    try
	    {
	        if ( fs.getDirectory() != null )
	        {
	            File directory = new File( fs.getDirectory() );
	            String includes = toString( fs.getIncludes() );
	            String excludes = toString( fs.getExcludes() );
	            return (List<File>)FileUtils.getFiles( directory, includes, excludes );
	        }
	        else
	        {
	            getLog().warn( String.format( "Fileset [%s] directory empty", fs.toString() ) );
	            return new ArrayList<>();
	        }
	    }
	    catch ( IOException e )
	    {
	        throw new MojoExecutionException( String.format( "Unable to get paths to fileset [%s]", fs.toString() ),
	                e );
	    }
	}
	
	
	private String toString( List<String> strings )
    {
        StringBuilder sb = new StringBuilder();
        for ( String string : strings )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ", " );
            }
            sb.append( string );
        }
        return sb.toString();
    }
}
