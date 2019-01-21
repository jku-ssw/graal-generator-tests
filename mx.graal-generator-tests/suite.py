suite = {
    "mxversion" : "5.199.0",
    "name" : "graal-generator-tests",

    "defaultLicense" : "GPLv2-CPE",

    "versionConflictResolution": "latest",

    "imports": {
        "suites": [
            {
                "name": "compiler",
                "subdir": True,
                "version": "f2916dbcc8a1e0412b98239bb625de0d7ee7841e",
                "urls" : [
                    {"url" : "https://github.com/graalvm/graal", "kind": "git"},
                    {"url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary"},
                ]
            }
        ]
    },

    "libraries" : {
        "JBGENERATOR" : {
            "urls" : [
                "https://github.com/jku-ssw/java-bytecode-generator/releases/download/v1.0.0/jbgenerator-1.0.0.jar"
            ],
            "sha1" : "50f69012583984849c5e5c5cd7ec85cd3653b85a",
        },
        "COMMONS_CLI": {
            "sha1": "c51c00206bb913cd8612b24abd9fa98ae89719b1",
            "maven": {
                "groupId": "commons-cli",
                "artifactId": "commons-cli",
                "version": "1.4",
            }
        }
    },

    "projects": {

        "at.jku.ssw.java.bytecode.generator.tests" : {
            "subDir" : "projects",
            "sourceDirs" : ["src"],
            "dependencies" : [
                "JBGENERATOR",
                "COMMONS_CLI",
                "compiler:GRAAL",
                "mx:JUNIT",
            ],
            "javaCompliance" : "8+",
            "workingSets" : "Graal, HotSpot, Test",
        },

    },

    "distributions": {
        "GRAAL_GENERATOR_TESTS": {
            "mainClass" : "at.jku.ssw.java.bytecode.generator.tests.CompileGeneratedClasses",
            "subDir" : "projects",
            "dependencies" : [
                "at.jku.ssw.java.bytecode.generator.tests"
            ],
            "exclude" : [
                "mx:JUNIT",
                "JBGENERATOR"
            ],
        }
    },
}
