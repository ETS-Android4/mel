package de.mein.drive.jobs;

import de.mein.Lok;
import de.mein.auth.jobs.Job;
import de.mein.drive.data.Commit;

/**
 * Created by xor on 4/24/17.
 */
public class CommitJob extends Job {

    private boolean syncAnyway = false;

    public CommitJob(boolean syncAnyway) {
        this.syncAnyway = syncAnyway;
    }

    public CommitJob() {
        Lok.debug("CommitJob.CommitJob");
    }

    public boolean getSyncAnyway() {
        return syncAnyway;
    }
}
