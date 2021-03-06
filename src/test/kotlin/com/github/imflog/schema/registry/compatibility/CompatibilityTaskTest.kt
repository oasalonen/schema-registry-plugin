package com.github.imflog.schema.registry.compatibility

import com.github.imflog.schema.registry.REGISTRY_FAKE_PORT
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.*
import java.io.File

class CompatibilityTaskTest {
    lateinit var folderRule: TemporaryFolder
    lateinit var buildFile: File

    companion object {
        lateinit var wireMockServerItem: WireMockServer

        @BeforeClass
        @JvmStatic
        fun initClass() {
            wireMockServerItem = WireMockServer(
                    WireMockConfiguration
                            .wireMockConfig()
                            .port(REGISTRY_FAKE_PORT)
                            .notifier(ConsoleNotifier(true)))
            wireMockServerItem.start()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            wireMockServerItem.stop()
        }
    }

    @Before
    fun init() {
        folderRule = TemporaryFolder()
        folderRule.create()
        wireMockServerItem.stubFor(
                WireMock.post(WireMock
                        .urlMatching("/compatibility/subjects/.*/versions/.*"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
                                .withBody("{\"is_compatible\": true}")))

        folderRule.newFolder("avro")
        val testAvsc = folderRule.newFile("avro/other_test.avsc")
        val schemaTest = """
            {
                "type":"record",
                "name":"Blah",
                "fields":[
                    {
                        "name":"name",
                        "type":"string"
                    }
                ]
            }
        """.trimIndent()
        testAvsc.writeText(schemaTest)

        val testAvsc2 = folderRule.newFile("avro/test.avsc")
        testAvsc2.writeText(schemaTest)
    }

    @After
    internal fun tearDown() {
        folderRule.delete()
    }

    @Test
    fun `CompatibilityTask should validate input schema`() {
        buildFile = folderRule.newFile("build.gradle")
        buildFile.writeText("""
            plugins {
                id 'java'
                id 'com.github.imflog.kafka-schema-registry-gradle-plugin'
            }

            schemaRegistry {
                url = 'http://localhost:$REGISTRY_FAKE_PORT/'
                compatibility {
                    subject('testSubject1', 'avro/test.avsc')
                    subject('testSubject2', 'avro/other_test.avsc')
                }
            }
        """)

        val result: BuildResult? = GradleRunner.create()
                .withGradleVersion("4.9")
                .withProjectDir(folderRule.root)
                .withArguments(TEST_SCHEMAS_TASK)
                .withPluginClasspath()
                .withDebug(true)
                .build()
        Assertions.assertThat(result?.task(":testSchemasTask")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}