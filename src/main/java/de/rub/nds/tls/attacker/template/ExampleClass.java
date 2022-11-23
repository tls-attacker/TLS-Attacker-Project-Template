/*
 * A new TLS-Attacker project. 
 * Adjust this text in license_header_plain.txt
 *
 * Copyright 2022
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.tls.attacker.template;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.protocol.message.ChangeCipherSpecMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.FinishedMessage;
import de.rub.nds.tlsattacker.core.protocol.message.RSAClientKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.DefaultWorkflowExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.GenericReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.action.WaitAction;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class ExampleClass {

    public static void main(String args[]) {
        if(args.length != 3) {
            System.out.println("Expecting three arguments: [IP] [Port] [ProtocolVersion]\n Example: java -jar apps/TLS-Attacker-Template.jar \"10.160.160.3\" \"50001\" \"TLS12\"");
            return;
        }
        // Make sure to add BouncyCastle as a security provider
        Security.addProvider(new BouncyCastleProvider());
        // This is an example TLS-Attacker application
        // Lets do some basic stuff
        // we create some basic config with default values
        Config config = Config.createConfig();
        // we specify where we want to connect to
        // you can change the runningmode to server and adjust the defaultServerConnection if you
        // want to run TLS-Attacker as a server
        config.setDefaultRunningMode(RunningModeType.CLIENT);
        config.getDefaultClientConnection().setHostname(args[0]);
        config.getDefaultClientConnection().setPort(Integer.parseInt(args[1]));
        config.getDefaultClientConnection().setTimeout(200);
        // We add some extensions
        config.setAddClientAuthzExtension(Boolean.TRUE);
        config.setAddHeartbeatExtension(Boolean.TRUE);
        // We specify some more parameters
        config.setDefaultClientSupportedCipherSuites(
                CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA);
        config.setHighestProtocolVersion(ProtocolVersion.fromString(args[2]));
        // Now let's specify a WorkflowTrace
        WorkflowTrace trace = new WorkflowTrace();
        // Send a ClientHello with the specified extensions
        trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
        // Receive Some messages (we dont know how the server will react
        trace.addTlsAction(new GenericReceiveAction());
        // Now let's wait 2 seconds (why not :) )
        trace.addTlsAction(new WaitAction(2000));
        // Now let's send an rsa client key exchange message + ccs + finished
        trace.addTlsAction(
                new SendAction(
                        new RSAClientKeyExchangeMessage(),
                        new ChangeCipherSpecMessage(),
                        new FinishedMessage()));
        // Let's see what the other party has to say about this
        trace.addTlsAction(new GenericReceiveAction());
        // Now let's send a ServerHello message after the Handshake was executed
        trace.addTlsAction(new SendAction(new ServerHelloMessage(config)));
        // Let's see what the other party has to say about this
        trace.addTlsAction(new GenericReceiveAction());

        // Let's execute the Trace
        State state = new State(config, trace);
        WorkflowExecutor executor = new DefaultWorkflowExecutor(state);
        executor.executeWorkflow();
        // Ok the trace was now executed. Let's analyze it
        System.out.println(
                "Received Finished:"
                        + WorkflowTraceUtil.didReceiveMessage(
                                HandshakeMessageType.FINISHED, trace));
        System.out.println(
                "Selected CipherSuite: " + state.getTlsContext().getSelectedCipherSuite());
    }
}
