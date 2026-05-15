import Foundation
import Combine
import Shared

/// Bridges a Kotlin `StateFlow<T>` (exposed via the KMP `Shared` framework)
/// to a Combine `Publisher` for SwiftUI consumption.
///
/// The "right" tool for this job is [SKIE](https://touchlab.co/skie), which
/// generates strongly-typed Swift wrappers for KMP flows automatically.
/// We avoid the dependency in the default scaffold and provide a small
/// hand-rolled bridge here. To upgrade later: install the SKIE plugin in
/// `shared/build.gradle.kts`, then replace this file with a one-liner
/// using `SkieSwiftStateFlow<T>`.
final class StateFlowPublisher<Output: AnyObject>: Publisher {
    typealias Failure = Never

    private let stateFlow: Kotlinx_coroutines_coreStateFlow

    /// `stateFlow` is the type-erased Kotlin StateFlow as seen from Swift.
    /// The caller is responsible for casting `Output` correctly.
    init(stateFlow: Kotlinx_coroutines_coreStateFlow) {
        self.stateFlow = stateFlow
    }

    func receive<S: Subscriber>(subscriber: S) where S.Input == Output, S.Failure == Never {
        let subject = PassthroughSubject<Output, Never>()

        // Initial value
        if let initial = stateFlow.value as? Output {
            subject.send(initial)
        }

        // Subscribe to the StateFlow's `collect` via a Closeable.
        // (Generated as `collect(collector:completionHandler:)` by Kotlin/Native.)
        let job = Task { @MainActor in
            do {
                try await StateFlowCollectorHelperKt.collectFlow(flow: stateFlow) { value in
                    if let typed = value as? Output {
                        subject.send(typed)
                    }
                }
            } catch {
                // StateFlow.collect never throws under normal operation.
            }
        }

        subject
            .handleEvents(receiveCancel: { job.cancel() })
            .receive(subscriber: subscriber)
    }
}
