class ShopApp {
    constructor() {
        this.cart = [];
        this.products = [];
        this.init();
    }

    async init() {
        await this.loadProducts();
        await this.loadCart();
        this.setupEventListeners();
        this.renderProducts();
        this.updateCartDisplay();
    }

    async loadProducts() {
        try {
            this.products = await API.getAllProducts();
            console.log('Loaded products:', this.products);
        } catch (error) {
            console.error('Failed to load products:', error);
            this.showError('Failed to load products. Please refresh the page.');
        }
    }

    async loadCart() {
        try {
            const cartItems = await API.getCart();
            this.cart = cartItems || [];
            console.log('Loaded cart:', this.cart);
        } catch (error) {
            console.error('Failed to load cart:', error);
            this.cart = [];
        }
    }

    renderProducts(productsToRender = this.products) {
        const grid = document.getElementById('product-grid');
        grid.innerHTML = '';

        if (productsToRender.length === 0) {
            grid.innerHTML = '<p>No products available</p>';
            return;
        }

        productsToRender.forEach(product => {
            const card = document.createElement('div');
            card.className = 'product-card';
            const isOutOfStock = product.quantity === 0;

            card.innerHTML = `
                <h3>${product.name}</h3>
                <p class="category">${product.category}</p>
                <p>${product.description}</p>
                <p class="price">$${product.price.toFixed(2)}</p>
                <p class="stock" style="color: ${isOutOfStock ? 'red' : '#7f8c8d'}">
                    ${product.quantity} in stock ${isOutOfStock ? '⚠️' : ''}
                </p>
                <input type="number" min="1" max="${product.quantity}" value="1" 
                       id="qty-${product.id}" ${isOutOfStock ? 'disabled' : ''}>
                <button class="btn-primary" onclick="app.addToCart('${product.id}')" 
                        ${isOutOfStock ? 'disabled' : ''}>
                    ${isOutOfStock ? 'Out of Stock' : 'Add to Cart'}
                </button>
            `;
            grid.appendChild(card);
        });
    }

    async addToCart(productId) {
        const qtyInput = document.getElementById(`qty-${productId}`);
        const quantity = parseInt(qtyInput.value);

        if (isNaN(quantity) || quantity < 1) {
            this.showError('Please enter a valid quantity');
            return;
        }

        // Check local stock first
        const product = this.products.find(p => p.id === productId);
        if (!product) {
            this.showError('Product not found');
            return;
        }

        if (product.quantity < quantity) {
            this.showError(`Only ${product.quantity} units available for ${product.name}`);
            return;
        }

        try {
            console.log('Adding to cart:', productId, quantity);
            const response = await API.addToCart(productId, quantity);
            console.log('Cart response:', response);

            this.cart = response;
            this.updateCartDisplay();

            // Reload products to get updated stock counts
            await this.loadProducts();
            this.renderProducts();

            this.showSuccess(`${product.name} added to cart!`);

            // Reset quantity input
            qtyInput.value = 1;
        } catch (error) {
            console.error('Add to cart error:', error);
            this.showError('Failed to add item: ' + error.message);
        }


        await this.loadProducts();
        this.renderProducts();
    }

    updateCartDisplay() {
        const cartCount = document.getElementById('cart-count');
        const totalItems = this.cart.reduce((sum, item) => sum + item.quantity, 0);
        cartCount.textContent = totalItems;

        // Update cart section
        const cartItems = document.getElementById('cart-items');
        if (this.cart.length === 0) {
            cartItems.innerHTML = '<p>Your cart is empty</p>';
            document.getElementById('cart-subtotal').textContent = '0.00';
            document.getElementById('cart-tax').textContent = '0.00';
            document.getElementById('cart-total').textContent = '0.00';
            return;
        }

        cartItems.innerHTML = '';
        let subtotal = 0;

        this.cart.forEach(item => {
            const itemTotal = item.product.price * item.quantity;
            subtotal += itemTotal;

            const cartItem = document.createElement('div');
            cartItem.className = 'cart-item';
            cartItem.innerHTML = `
                <div>
                    <h4>${item.product.name}</h4>
                    <p>$${item.product.price.toFixed(2)} x ${item.quantity}</p>
                </div>
                <div>
                    <strong>$${itemTotal.toFixed(2)}</strong>
                    <button class="btn-secondary" onclick="app.removeFromCart('${item.product.id}')">Remove</button>
                </div>
            `;
            cartItems.appendChild(cartItem);
        });

        const tax = subtotal * 0.10;
        const total = subtotal + tax;

        document.getElementById('cart-subtotal').textContent = subtotal.toFixed(2);
        document.getElementById('cart-tax').textContent = tax.toFixed(2);
        document.getElementById('cart-total').textContent = total.toFixed(2);
    }

    async removeFromCart(productId) {
        try {
            console.log('Removing from cart:', productId);
            const response = await API.removeFromCart(productId);
            this.cart = response;
            this.updateCartDisplay();

            // Reload products to restore stock counts
            await this.loadProducts();
            this.renderProducts();

            this.showSuccess('Item removed from cart');
        } catch (error) {
            console.error('Remove from cart error:', error);
            this.showError('Failed to remove item: ' + error.message);
        }
    }

    showSection(sectionId) {
        // Hide all sections
        document.getElementById('products').classList.add('hidden');
        document.getElementById('cart').classList.add('hidden');

        // Show requested section
        document.getElementById(sectionId).classList.remove('hidden');
    }

    // NEW: Show error message to user
    showError(message) {
        // Create toast notification
        const toast = document.createElement('div');
        toast.className = 'toast toast-error';
        toast.textContent = '❌ ' + message;
        document.body.appendChild(toast);

        // Auto remove after 4 seconds
        setTimeout(() => {
            toast.classList.add('toast-fade-out');
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    // NEW: Show success message to user
    showSuccess(message) {
        const toast = document.createElement('div');
        toast.className = 'toast toast-success';
        toast.textContent = '✓ ' + message;
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.classList.add('toast-fade-out');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    setupEventListeners() {
        // Search
        document.getElementById('search').addEventListener('input', (e) => {
            const term = e.target.value.toLowerCase();
            const filtered = this.products.filter(p =>
                p.name.toLowerCase().includes(term) ||
                p.description.toLowerCase().includes(term)
            );
            this.renderProducts(filtered);
        });

        // Category filter
        document.getElementById('category-filter').addEventListener('change', async (e) => {
            if (e.target.value === '') {
                this.renderProducts();
            } else {
                try {
                    const filtered = await API.searchProducts(e.target.value);
                    this.renderProducts(filtered);
                } catch (error) {
                    console.error('Filter error:', error);

                    const filtered = this.products.filter(p => p.category === e.target.value);
                    this.renderProducts(filtered);
                }
            }
        });

        // Navigation - Products link
        const productsLinks = document.querySelectorAll('a[href="#products"]');
        productsLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                this.showSection('products');
            });
        });

        // Navigation - Cart link
        document.getElementById('cart-link').addEventListener('click', (e) => {
            e.preventDefault();
            this.showSection('cart');
        });

        // Navigation - Account link (placeholder)
        const accountLinks = document.querySelectorAll('a[href="#account"]');
        accountLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                this.showError('Account section coming soon!');
            });
        });

        // Checkout
        document.getElementById('checkout-btn').addEventListener('click', () => {
            if (this.cart.length === 0) {
                this.showError('Your cart is empty');
                return;
            }
            document.getElementById('checkout-modal').classList.remove('hidden');
        });

        document.getElementById('cancel-checkout').addEventListener('click', () => {
            document.getElementById('checkout-modal').classList.add('hidden');
        });

        document.getElementById('checkout-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.processCheckout();
        });
    }

    async processCheckout() {
        const customerData = {
            name: document.getElementById('customer-name').value,
            email: document.getElementById('customer-email').value,
            phone: document.getElementById('customer-phone').value,
            address: document.getElementById('customer-address').value
        };

        const paymentMethod = document.getElementById('payment-method').value;

        if (!paymentMethod) {
            this.showError('Please select a payment method');
            return;
        }

        // Show loading state
        const submitBtn = document.querySelector('#checkout-form button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Processing...';

        try {
            console.log('Processing checkout...', customerData);
            const invoice = await API.checkout(customerData, paymentMethod);
            console.log('Checkout response:', invoice);

            if (invoice.success) {
                // Hide checkout modal
                document.getElementById('checkout-modal').classList.add('hidden');

                // Show invoice
                this.displayInvoice(invoice);

                // Clear cart
                this.cart = [];
                this.updateCartDisplay();

                // Clear form
                document.getElementById('checkout-form').reset();

                // Reload products
                await this.loadProducts();
                this.renderProducts();
            } else {
                this.showError('Checkout failed: ' + (invoice.error || 'Unknown error'));
            }

        } catch (error) {
            console.error('Checkout error:', error);
            this.showError('Checkout failed: ' + error.message);
        } finally {
            // Restore button state
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        }
    }

    displayInvoice(invoice) {
        const content = document.getElementById('invoice-content');
        content.innerHTML = `
            <div style="text-align: center;">
                <h3 style="color: #27ae60; margin-bottom: 1rem;">✓ Order Confirmed!</h3>
                <p><strong>Order #${invoice.orderId}</strong></p>
                <p>Thank you for your order!</p>
                <hr style="margin: 1.5rem 0; border: none; border-top: 1px solid #ddd;">
                <div style="text-align: left; background: #f8f9fa; padding: 1rem; border-radius: 4px; margin: 1rem 0;">
                    <p><strong>Tracking Number:</strong><br>${invoice.trackingNumber}</p>
                    <p><strong>Total Paid:</strong><br>$${invoice.total.toFixed(2)}</p>
                </div>
                <hr style="margin: 1.5rem 0; border: none; border-top: 1px solid #ddd;">
                <p style="color: #7f8c8d;">Your order will be delivered to the address provided.</p>
            </div>
        `;
        document.getElementById('invoice-modal').classList.remove('hidden');

        document.getElementById('close-invoice').onclick = () => {
            document.getElementById('invoice-modal').classList.add('hidden');
            // Go back to products page
            this.showSection('products');
        };
    }
}

// Initialize app
const app = new ShopApp();