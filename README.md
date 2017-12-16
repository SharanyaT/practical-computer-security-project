# practical-computer-security-project
Source code for our team project for the course practical computer security

In order to run this program, you need to override the security configuration files in your java installation files. Specifically, the policies - US export policy.jar and local policy.jar. Please note however that this will make your java environment highly insecure. 

Create /lib folder and add most recent BouncyCastle jar from http://www.bouncycastle.org/latestreleases.html
Make sure your IDE is configured to use the /lib folder. Don't try to automatically get it through Maven because
it wasn't signed there for some reason and Java will refuse to run unsigned crypto libraries.

Download JCE from http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html and put the two
.jar files in your local java installation (overwrite the ones already there) because Java will refuse to run anything
more than most basic cryptoraphic algorithms out of the box. Also please note that running under this configuration is illegal and this was still implemented purely for the purpose of this course project.

The server can be launched by running the file Server.java and client sessions can be initiated using the file ChatFrame.java.
