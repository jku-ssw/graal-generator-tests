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
                "version": "7a44af07d4dc34684c72648859196ea6c5e8f027",
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
    },

    "projects": {

        "at.jku.ssw.java.bytecode.generator.tests" : {
            "subDir" : "projects",
            "sourceDirs" : ["src"],
            "dependencies" : [
                "JBGENERATOR",
                "compiler:GRAAL",
                "mx:JUNIT",
            ],
            "javaCompliance" : "8+",
            "workingSets" : "Graal, HotSpot, Test",
        },

    },

    "distributions": {
        "CGEN": {
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
