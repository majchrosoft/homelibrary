package com.majchrosoft.homelibrary.domain.model

// Borrow state is now embedded directly on each [Item] under [BorrowState].
// A separate `Loan` aggregate will return when we add cross-user lending
// (one user requesting an item from another). Until then this file is
// intentionally empty so the model matches the Firebase tree exactly.
