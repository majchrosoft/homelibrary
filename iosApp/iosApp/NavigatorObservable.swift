import Foundation
import Combine
import Shared

/// SwiftUI bridge over the KMP `Navigator`. Subscribes to its `current`
/// StateFlow, republishes as `@Published`, and exposes the navigation API.
///
/// One instance per app session — the underlying `Navigator` is a Koin
/// singleton so all views agree on the current screen.
@MainActor
final class NavigatorObservable: ObservableObject {
    @Published private(set) var current: Screen = Screen.Library()
    private let navigator: Navigator
    private var cancellable: AnyCancellable?

    init() {
        navigator = KoinHelperKt.resolveNavigator()
        cancellable = StateFlowPublisher<Screen>(stateFlow: navigator.current)
            .sink { [weak self] screen in self?.current = screen }
    }

    func push(_ screen: Screen) { navigator.push(screen: screen) }
    func replace(_ screen: Screen) { navigator.replace(screen: screen) }
    func back() { navigator.back() }
    func popToRoot() { navigator.popToRoot() }
}
