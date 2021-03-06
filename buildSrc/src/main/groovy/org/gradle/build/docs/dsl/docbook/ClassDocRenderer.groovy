/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.build.docs.dsl.docbook

import org.gradle.build.docs.dsl.docbook.model.ClassDoc
import org.gradle.build.docs.dsl.docbook.model.ClassExtensionDoc
import org.w3c.dom.Element

class ClassDocRenderer {
    private final LinkRenderer linkRenderer
    private final GenerationListener listener = new DefaultGenerationListener()
    private final PropertyTableRenderer propertyTableRenderer = new PropertyTableRenderer()
    private final MethodTableRenderer methodTableRenderer = new MethodTableRenderer()
    private final ClassDescriptionRenderer descriptionRenderer = new ClassDescriptionRenderer()
    private final PropertyDetailRenderer propertiesDetailRenderer
    private final MethodDetailRenderer methodDetailRenderer
    private final BlockDetailRenderer blockDetailRenderer
    private final ExtensionPropertiesSummaryRenderer extensionPropertiesSummaryRenderer

    ClassDocRenderer(LinkRenderer linkRenderer) {
        this.linkRenderer = linkRenderer
        propertiesDetailRenderer = new PropertyDetailRenderer(linkRenderer, listener)
        methodDetailRenderer = new MethodDetailRenderer(linkRenderer, listener)
        blockDetailRenderer = new BlockDetailRenderer(linkRenderer, listener)
        extensionPropertiesSummaryRenderer = new ExtensionPropertiesSummaryRenderer(propertyTableRenderer)
    }

    void mergeContent(ClassDoc classDoc, Element parent) {
        listener.start("class $classDoc.name")
        try {
            def chapter = parent.ownerDocument.createElement("chapter")
            parent.appendChild(chapter)
            chapter.setAttribute('id', classDoc.id)
            descriptionRenderer.renderTo(classDoc, chapter)
            mergeProperties(classDoc, chapter)
            mergeBlocks(classDoc, chapter)
            mergeMethods(classDoc, chapter)
        } finally {
            listener.finish()
        }
    }

    void mergeProperties(ClassDoc classDoc, Element classContent) {

        Element propertiesSummarySection = classContent << {
            section {
                title('Properties')
            }
        }

        boolean hasProperties = false
        def classProperties = classDoc.classProperties
        if (!classProperties.isEmpty()) {
            hasProperties = true
            def propertiesTable = propertiesSummarySection << {
                table {
                    title("Properties - $classDoc.simpleName")
                }
            }
            propertyTableRenderer.renderTo(classProperties, propertiesTable)
        }

        classDoc.classExtensions.each { ClassExtensionDoc extension ->
            hasProperties |= !extension.extensionProperties.empty
            extensionPropertiesSummaryRenderer.renderTo(extension, propertiesSummarySection)
        }

        if (!hasProperties) {
            propertiesSummarySection << { para('No properties') }
            return
        }

        Element propertiesDetailSection = classContent << {
            section {
                title('Property details')
            }
        }
        classProperties.each { propDoc ->
            propertiesDetailRenderer.renderTo(propDoc, propertiesDetailSection)
        }

        classDoc.classExtensions.each { ClassExtensionDoc extension ->
            extension.extensionProperties.each { propDoc ->
                propertiesDetailRenderer.renderTo(propDoc, propertiesDetailSection)
            }
        }
    }

    void mergeMethods(ClassDoc classDoc, Element classContent) {
        Element methodsSummarySection = classContent << {
            section { title('Methods') }
        }

        def classMethods = classDoc.classMethods
        if (classMethods.isEmpty()) {
            methodsSummarySection << {
                para('No methods')
            }
            return
        }

        def table = methodsSummarySection << {
            table {
                title("Methods - $classDoc.simpleName")
            }
        }
        methodTableRenderer.renderTo(classMethods, table)

        Element methodsDetailSection = classContent << {
            section {
                title('Method details')
            }
        }
        classMethods.each { method ->
            methodDetailRenderer.renderTo(method, methodsDetailSection)
        }

        mergeExtensionMethods(classDoc, methodsSummarySection, methodsDetailSection)
    }

    void mergeBlocks(ClassDoc classDoc, Element classContent) {
        Element blocksSummarySection = classContent << {
            section {
                title('Script blocks')
            }
        }

        def classBlocks = classDoc.classBlocks
        if (classBlocks.isEmpty()) {
            blocksSummarySection << {
                para('No script blocks')
            }
            return
        }

        blocksSummarySection << {
            table {
                title("Script blocks - $classDoc.simpleName")
                thead {
                    tr {
                        td('Block'); td('Description')
                    }
                }
                classBlocks.each { block ->
                    tr {
                        td { link(linkend: block.id) { literal(block.name) } }
                        td { appendChild block.description }
                    }
                }
            }
        }
        Element blocksDetailSection = classContent << {
            section {
                title('Script block details')
            }
        }
        classBlocks.each { block ->
            blockDetailRenderer.renderTo(block, blocksDetailSection)
        }

        mergeExtensionBlocks(classDoc, blocksSummarySection, blocksDetailSection)
    }

    void mergeExtensionMethods(ClassDoc classDoc, Element summaryParent, Element detailParent) {
        classDoc.classExtensions.each { ClassExtensionDoc extension ->
            if (!extension.extensionMethods) {
                return
            }

            def summarySection = summaryParent << {
                section {
                    title { text("Methods added by the "); literal(extension.pluginId); text(" plugin") }
                    titleabbrev { literal(extension.pluginId); text(" plugin") }
                }
            }
            def summaryTable = summarySection << {
                table {
                    title { text("Methods - "); literal(extension.pluginId); text(" plugin") }
                }
            }
            methodTableRenderer.renderTo(extension.extensionMethods, summaryTable)

            extension.extensionMethods.each { method ->
                methodDetailRenderer.renderTo(method, detailParent)
            }
        }
    }

    void mergeExtensionBlocks(ClassDoc classDoc, Element summaryParent, Element detailParent) {
        classDoc.classExtensions.each { ClassExtensionDoc extension ->
            summaryParent << {
                if (!extension.extensionBlocks) {
                    return
                }
                section {
                    title { text("Script blocks added by the "); literal(extension.pluginId); text(" plugin") }

                    titleabbrev { literal(extension.pluginId); text(" plugin") }
                    table {
                        title { text("Script blocks - "); literal(extension.pluginId); text(" plugin") }
                        thead { tr { td('Block'); td('Description') } }
                        extension.extensionBlocks.each { blockDoc ->
                            tr {
                                td { link(linkend: blockDoc.id) { literal(blockDoc.name) } }
                                td { appendChild blockDoc.description }
                            }
                        }
                    }
                }
            }
            extension.extensionBlocks.each { block ->
                blockDetailRenderer.renderTo(block, detailParent)
            }
        }
    }
}


