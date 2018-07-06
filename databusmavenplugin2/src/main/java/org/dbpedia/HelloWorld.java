package org.dbpedia;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Mojo( name = "helloworld")
public class HelloWorld extends AbstractMojo
{
    @Parameter
    private String maintainer;

    public void execute() throws MojoExecutionException
    {
        File f = new File("./src/main/resources");

        if ( !f.exists() )
        {
            f.mkdirs();
        }

        File output = new File( f, "HelloWorld.txt" );

        FileWriter w = null;
        try
        {
            w = new FileWriter( output );

            w.write( "Maintainer: " +maintainer );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating file " + output, e );
        }
        finally
        {
            if ( w != null )
            {
                try
                {
                    w.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }
}
