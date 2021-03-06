/**
 * 
 */
package repast.simphony.batch.ssh;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import repast.simphony.batch.BatchConstants;
import repast.simphony.batch.RunningStatus;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

/**
 * Reads the run status from a remote and sets that on the Remote.
 * 
 * @author Nick Collier
 */
public class RemoteStatusGetter {

  public void run(RemoteSession remote, String remoteDir) throws StatusException { 

    //File tempDir = new File(System.getProperty("java.io.tmpdir"));
    
    SSHSession session = null;
    File file = null;
    
    try {
      Path tmp = Files.createTempDirectory(null);
      session = SSHSessionFactory.getInstance().create(remote);
      file = session.copyFileFromRemote(tmp.toFile().getPath().replace("\\", "/"),
          new File(remoteDir + "/" + BatchConstants.STATUS_OUTPUT_FILE), false);
      Properties props = new Properties();
      props.load(new FileReader(file));
      for (String key : props.stringPropertyNames()) {
        // key should be int and value is status
        remote.setRunStatus(Integer.valueOf(key), RunningStatus.valueOf(props.getProperty(key)));
      }
    } catch (SftpException e) {
    	e.printStackTrace();
      String msg = String.format("Error while copying status output file from %s", remote.getId());
      throw new StatusException(msg, e);
      
    } catch (JSchException e) {
      String msg = String.format("Error while creating connection to %s", remote.getId());
      throw new StatusException(msg, e);
   
    } catch (IOException e) {
      String msg = String.format("Error reading status file '%s' from remote %s", file.getPath(), remote.getId());
      throw new StatusException(msg, e);
      
    } finally {
      if (session != null)
        session.disconnect();
    }
  }
}
