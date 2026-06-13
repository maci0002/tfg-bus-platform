import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ContactService } from '../../core/services/contact.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    TranslateModule,
  ],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.scss',
})
export class ContactComponent {
  private fb        = inject(FormBuilder);
  private contact   = inject(ContactService);
  private snack     = inject(MatSnackBar);
  private translate = inject(TranslateService);

  loading = signal(false);
  sent    = signal(false);

  form: FormGroup = this.fb.group({
    name:    ['', [Validators.required, Validators.maxLength(120)]],
    email:   ['', [Validators.required, Validators.email]],
    subject: ['', [Validators.required, Validators.maxLength(150)]],
    message: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);

    this.contact.send(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.sent.set(true);
        this.form.reset();
        this.snack.open(this.translate.instant('contact.success'), this.translate.instant('contact.close'),
          { duration: 5000 });
      },
      error: () => {
        this.loading.set(false);
        this.snack.open(this.translate.instant('contact.error'), this.translate.instant('contact.close'),
          { panelClass: 'snack-error' });
      },
    });
  }

  get name()    { return this.form.get('name')!; }
  get email()   { return this.form.get('email')!; }
  get subject() { return this.form.get('subject')!; }
  get message() { return this.form.get('message')!; }
}
