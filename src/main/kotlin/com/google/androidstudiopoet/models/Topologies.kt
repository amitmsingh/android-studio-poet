/*
Copyright 2017 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.google.androidstudiopoet.models

import com.google.androidstudiopoet.input.ConfigPOJO
import java.security.InvalidParameterException
import java.util.*
import kotlin.collections.LinkedHashSet

/**
 * Enum with all supported topologies
 */
enum class Topologies {
    FULL {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<FromToDependencyConfig> {
            initialize(parameters, configPOJO)
            checkUnusedParameters(parameters)
            val result = mutableListOf<FromToDependencyConfig>()
            for (from in 0 until allNames.size) {
                val fromName = allNames[from]
                for (to in from + 1 until allNames.size) {
                    if (random.nextFloat() < density) {
                        result.add(FromToDependencyConfig(fromName, allNames[to], method))
                    }
                }
            }
            return result
        }
    },

    CONNECTED {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<FromToDependencyConfig> {
            initialize(parameters, configPOJO)
            checkUnusedParameters(parameters)
            val result = mutableListOf<FromToDependencyConfig>()
            var to = 1
            while (to < allNames.size) {
                var numFrom = 0
                val toName = allNames[to]
                for (from in 0 until to) {
                    if (random.nextFloat() < density) {
                        result.add(FromToDependencyConfig(allNames[from], toName, method))
                        numFrom++
                    }
                }
                if (numFrom > 0) {
                    to++
                }
            }
            return result
        }
    },

    LINEAR {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<FromToDependencyConfig> {
            initialize(parameters, configPOJO)
            checkUnusedParameters(parameters)
            val result = mutableListOf<FromToDependencyConfig>()
            for (id in 1 until allNames.size) {
                if (random.nextFloat() < density) {
                    result.add(FromToDependencyConfig(allNames[id - 1], allNames[id], method))
                }
            }
            return result
        }
    },

    STAR {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<FromToDependencyConfig> {
            initialize(parameters, configPOJO)
            checkUnusedParameters(parameters)
            val result = mutableListOf<FromToDependencyConfig>()
            for (id in 1 until allNames.size) {
                if (random.nextFloat() < density) {
                    result.add(FromToDependencyConfig(allNames[0], allNames[id], method))
                }
            }
            return result
        }
    },

    TREE {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<FromToDependencyConfig> {
            initialize(parameters, configPOJO)
            val degree = getDegree(parameters)
            checkUnusedParameters(parameters)

            val result = mutableListOf<FromToDependencyConfig>()
            for (id in 1 until allNames.size) {
                if (random.nextFloat() < density) {
                    result.add(FromToDependencyConfig(allNames[getParent(id, degree)], allNames[id], method))
                }
            }
            return result
        }

        private fun getParent(node: Int, degree: Int) = (node - 1) / degree

        private fun getDegree(parameters: Map<String, String>): Int {
            val degree = getRequiredParameter("degree", parameters).toInt()
            if (degree < 1) {
                throw InvalidParameterException("Degree must be a positive integer: $parameters")
            }
            return degree
        }
    },

    VARIABLE_TREE {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<FromToDependencyConfig> {
            initialize(parameters, configPOJO)
            val wideness = getWideness(parameters)
            checkUnusedParameters(parameters)

            val result = mutableListOf<FromToDependencyConfig>()
            var currentParent = 0
            for (to in 1 until allNames.size) {
                if (random.nextFloat() < density) {
                    result.add(FromToDependencyConfig(allNames[currentParent], allNames[to], method))
                }
                if (random.nextFloat() >= wideness) {
                    currentParent++
                }
            }
            return result
        }

        private fun getWideness(parameters: Map<String, String>): Float {
            val wideness = getRequiredParameter("wideness", parameters).toFloat()
            if (wideness < 0.0f || wideness > 1.0f) {
                throw InvalidParameterException("Wideness should be in [0.0, 1.0]: $parameters")
            }
            return wideness
        }
    },

    RECTANGLE {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<FromToDependencyConfig> {
            initialize(parameters, configPOJO)
            val width = getWidth(parameters)
            checkUnusedParameters(parameters)

            val result = mutableListOf<FromToDependencyConfig>()
            for (to in width until allNames.size) {
                val base = ((to / width) - 1) * width
                for (from in 0 until width) {
                    if (random.nextFloat() < density) {
                        result.add(FromToDependencyConfig(allNames[base + from], allNames[to], method))
                    }
                }
            }
            return result
        }
    },

    CONNECTED_RECTANGLE {
        override fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<FromToDependencyConfig> {
            initialize(parameters, configPOJO)
            val width = getWidth(parameters)
            checkUnusedParameters(parameters)

            val result = mutableListOf<FromToDependencyConfig>()
            var to = width
            while (to < allNames.size) {
                val base = ((to / width) - 1) * width
                var numFrom = 0
                for (from in 0 until width) {
                    if (random.nextFloat() < density) {
                        result.add(FromToDependencyConfig(allNames[base + from], allNames[to], method))
                        numFrom++
                    }
                }
                if (numFrom > 0) {
                    to++
                }
            }
            return result
        }
    };

    // Common parameters
    protected var density: Float = 1.0f
    protected var random: Random = Random()
    protected var method: String? = null

    private var validParameters: LinkedHashSet<String> = linkedSetOf()
    protected val allNames: MutableList<String> = mutableListOf()

    protected fun initialize(parameters: Map<String, String>, configPOJO: ConfigPOJO) {
        // Clear list of valid parameters
        validParameters.clear()
        validParameters.add("type")
        // Take common parameters
        density = getOptionalParameter("density", parameters)?.toFloat() ?: 1.0f
        random = randomWithSeed(parameters)
        method = getOptionalParameter("method", parameters)
        // Generate a list with all module names
        generateModuleNames(configPOJO)
    }

    private fun getOptionalParameter(name: String, parameters: Map<String, String>): String? {
        validParameters.add(name)
        return parameters[name]
    }

    protected fun randomWithSeed(parameters: Map<String, String>) : Random {
        val seedInput = getOptionalParameter("seed", parameters)
        val seed : Long = seedInput?.toLong() ?: 0
        return Random(seed)
    }

    protected fun generateModuleNames( configPOJO: ConfigPOJO) {
        allNames.clear()
        (0 until configPOJO.androidModules).mapTo(allNames) {getAndroidModuleNameByIndex(it)}
        (0 until configPOJO.numModules).mapTo(allNames) {getModuleNameByIndex(it)}
    }

    protected fun getRequiredParameter(name: String, parameters: Map<String, String>): String {
        validParameters.add(name)
        val result = parameters[name]
        if (result == null) {
            throw InvalidParameterException("Required parameter \"$name\" was not specified: $parameters")
        }
        else {
            return result
        }
    }

    protected fun getWidth(parameters: Map<String, String>): Int {
        val width = getRequiredParameter("width", parameters).toInt()
        if (width <= 0) {
            throw InvalidParameterException("width must be greater than 0: $parameters")
        }
        return width
    }

    protected fun checkUnusedParameters(parameters: Map<String, String>) {
        val notUsed = parameters.filter { entry -> !validParameters.contains(entry.key) }
        if (notUsed.isNotEmpty()) {
            throw InvalidParameterException("The following parameters are not used: $notUsed")
        }
    }

    private fun getAndroidModuleNameByIndex(index: Int) = "androidAppModule$index"

    private fun getModuleNameByIndex(index: Int) = "module$index"

    /**
     * Function that should add dependencies to configPOJO based on the given parameters and the
     * content of configPOJO
     */
    abstract fun generateDependencies(parameters: Map<String, String>, configPOJO: ConfigPOJO): List<FromToDependencyConfig>
}
