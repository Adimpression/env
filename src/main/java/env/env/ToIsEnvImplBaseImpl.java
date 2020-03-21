package env.env;

import env.input.IsInput;
import env.output.IsOutput;
import io.grpc.stub.StreamObserver;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

public class ToIsEnvImplBaseImpl extends ToIsEnvGrpc.ToIsEnvImplBase {

    private final Logger log;

    public ToIsEnvImplBaseImpl() {
        log = Logger.getLogger(getClass().getName());
        log.info(" starting...");

        log.info(" started");
    }

    @Override
    public void produce(final NotEnv request, final StreamObserver<IsEnv> responseObserver) {
        final IsInput isInput = request.getIsInput();
        final String isVariableString = isInput.getIsVariableString();
        final String isDomainString = isInput.getIsDomainString();
        final String environmentVariableValue = System.getenv(isVariableString);
        if (environmentVariableValue != null && !environmentVariableValue.isEmpty()) {
            responseObserver.onNext(IsEnv.newBuilder()
                    .setIsOutput(IsOutput.newBuilder()
                            .setIsValueString(environmentVariableValue)
                            .build())
                    .build());
            responseObserver.onCompleted();
        } else {
            if (isDomainString.isEmpty()) {
                throw new IllegalStateException("Environment variable " + isVariableString + " was not found and domain was not set either");
            } else {
                final String environmentVariable = isDomainString.replaceAll("\\.",
                        "_");
                String appDomainFile = System.getenv(environmentVariable);
                if (appDomainFile == null || appDomainFile.isEmpty()) {
                    appDomainFile = "/etc/dimensions/" + isDomainString + ".register.yaml";
                }

                try {
                    final String content = FileUtils.readFileToString(new File(appDomainFile),
                            "UTF-8");
                    final Map<String, Object> yaml = new Yaml().load(content);
                    final String profile = System.getenv("profile");
                    if (profile == null || profile.isEmpty()) {
                        throw new IllegalStateException("Environment variable " + "profile" + " was not found.");
                    } else {
                        final Map<String, Object> artifact = (Map<String, Object>) yaml.get("artifact");
                        final Map<String, Object> conf = (Map<String, Object>) artifact.get("conf");
                        final Map<String, Object> confProfile = (Map<String, Object>) conf.get(profile);
                        if (!confProfile.containsKey(isVariableString)) {
                            throw new IllegalStateException("Environment variable " + isVariableString + " was not found in as an environment variable and in file " + appDomainFile);
                        }
                        final Object v = confProfile.get(isVariableString);
                        responseObserver.onNext(IsEnv.newBuilder()
                                .setIsOutput(IsOutput.newBuilder()
                                        .setIsValueString(v.toString())
                                        .build())
                                .build());
                        responseObserver.onCompleted();
                    }
                } catch (Throwable e) {
                    throw new RuntimeException("Key: " + isVariableString + ". " + "File: " + appDomainFile,
                            e);
                }
            }
        }
    }
}
