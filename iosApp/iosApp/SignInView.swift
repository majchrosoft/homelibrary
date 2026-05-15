import SwiftUI
import Shared

struct SignInView: View {
    @ObservedObject var viewModel: AuthViewModelObservable
    @State private var email = ""
    @State private var password = ""
    @State private var isSignUp = false
    @State private var displayName = ""

    var body: some View {
        VStack(spacing: 16) {
            Text(isSignUp ? "Create your account" : "Sign in to Home Library")
                .font(.title2)

            if isSignUp {
                TextField("Display name", text: $displayName)
                    .textFieldStyle(.roundedBorder)
            }

            TextField("Email", text: $email)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .textFieldStyle(.roundedBorder)

            SecureField("Password", text: $password)
                .textFieldStyle(.roundedBorder)

            if let err = viewModel.state.errorMessage {
                Text(err).foregroundColor(.red).font(.caption)
            }

            Button(isSignUp ? "Create account" : "Sign in") {
                if isSignUp {
                    viewModel.signUp(email: email, password: password, displayName: displayName.isEmpty ? nil : displayName)
                } else {
                    viewModel.signIn(email: email, password: password)
                }
            }
            .disabled(viewModel.state.isLoading || email.isEmpty || password.isEmpty)
            .buttonStyle(.borderedProminent)

            Button(isSignUp ? "Already have an account? Sign in" : "New here? Create an account") {
                isSignUp.toggle()
            }
            .font(.caption)
        }
        .padding(24)
    }
}
