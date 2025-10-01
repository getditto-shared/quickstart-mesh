package live.ditto.quickstart.tasks

interface DittoCollectionSubscription {
    val subscriptionQuery: String
    val subscriptionQueryArgs: Map<String, Any>
}