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
            "path" : "lib/jbgenerator.jar",
            "sha1" : "a3a7c0b7609d7bae06489ed321027f6e4bd81c68",
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
