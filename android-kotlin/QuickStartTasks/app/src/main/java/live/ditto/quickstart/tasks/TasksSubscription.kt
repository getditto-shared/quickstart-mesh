package live.ditto.quickstart.tasks

class TasksSubscription(
    override val subscriptionQuery: String = """SELECT * FROM tasks
                                        WHERE NOT deleted""".trimIndent(),
    override val subscriptionQueryArgs: Map<String, Any> = emptyMap()
) : DittoCollectionSubscription