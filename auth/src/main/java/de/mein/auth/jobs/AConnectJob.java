package de.mein.auth.jobs;


import de.mein.Lok;
import de.mein.auth.tools.Eva;

/**
 * Created by xor on 12/13/16.
 */
public abstract class AConnectJob<R, P> extends Job<R, Exception, P> {
    private Long certificateId;
    private String address;
    private Integer port, portCert;

    public AConnectJob(Long certificateId, String address, Integer port, Integer portCert) {
        this.certificateId = certificateId;
        this.address = address;
        this.port = port;
        this.portCert = portCert;
        //todo debug
        if (port == 8888) {
            Eva.flag("ccc");
            Lok.debug("debug ccc=" + Eva.getFlagCount("ccc"));
            if (Eva.getFlagCount("ccc") > 12)
                Lok.debug();
        }
    }

    public AConnectJob setCertificateId(Long certificateId) {
        this.certificateId = certificateId;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getPortCert() {
        return portCert;
    }

    public Long getCertificateId() {
        return certificateId;
    }

    public String getAddress() {
        return address;
    }

}
