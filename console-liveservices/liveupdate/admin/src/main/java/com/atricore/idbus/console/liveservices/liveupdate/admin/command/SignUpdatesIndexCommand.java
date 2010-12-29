package com.atricore.idbus.console.liveservices.liveupdate.admin.command;

import com.atricore.idbus.console.liveservices.liveupdate.admin.service.LiveUpdateAdminService;
import com.atricore.liveservices.liveupdate._1_0.util.LiveUpdateKeyResolver;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import java.io.FileNotFoundException;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
@Command(scope = "liveupdate-admin", name = "sign-updates-index", description = "Sign updates index")
public class SignUpdatesIndexCommand extends SignValidateCommandSupport {

    @Option(name = "-f", aliases = "--file", description = "Updates index file", required = true, multiValued = false)
    private String updatesIndexFile;

    @Option(name = "-s", aliases = "--signed-file", description = "Signed updates index file", required = true, multiValued = false)
    private String signedUpdatesIndexFile;

    @Option(name = "-r", aliases = "--replace", description = "Replace destination file", required = false, multiValued = false)
    private boolean replace;

    @Override
    protected Object doExecute(LiveUpdateAdminService svc) throws Exception {
        byte[] updatesIndex;
        LiveUpdateKeyResolver keyResolver;
        try {
            updatesIndex = readContent(updatesIndexFile);
            keyResolver = getLiveUpdateKeyResolver();
        } catch (FileNotFoundException e) {
            System.err.println("\u001B[31mFile not found: " + e.getMessage() + "\u001B[0m");
            return null;
        }

        byte[] signedUpdatesIndex = svc.signUpdatesIndex(updatesIndex, keyResolver);
        writeContent(signedUpdatesIndexFile, signedUpdatesIndex, replace);

        System.out.println("Updates index file successfully signed.");

        return null;
    }
}
