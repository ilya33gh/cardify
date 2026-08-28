package com.cardify.app.ui.security

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.cardify.app.R
import com.cardify.app.ui.theme.ManropeFamily
import com.cardify.app.ui.theme.PillShape

@Composable
fun BiometricAuthLockScreen(
    isLocked: Boolean,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(isLocked) {
        if (isLocked && activity != null) {
            val manager = BiometricPromptManager(activity)
            if (manager.canAuthenticate()) {
                manager.showBiometricPrompt { result ->
                    if (result is BiometricResult.Success) {
                        onUnlockSuccess()
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isLocked,
        enter = fadeIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.lock_screen_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.lock_screen_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (activity != null) {
                            val manager = BiometricPromptManager(activity)
                            manager.showBiometricPrompt { result ->
                                if (result is BiometricResult.Success) {
                                    onUnlockSuccess()
                                }
                            }
                        }
                    },
                    shape = PillShape,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .widthIn(max = 380.dp)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.unlock_button_action),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }
            }
        }
    }
}
