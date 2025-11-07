class ShopApp {
    constructor() {
        this.cart = [];
        this.products = [];
        this.currentUser = null;
        this.categories = new Set();
        this.init();
    }

    async init() {
        await this.loadProducts();
        await this.loadCart();
        this.setupEventListeners();
        this.renderProducts();
        this.updateCartDisplay();
        this.populateCategories();
        this.checkLoginStatus();
    }

    async loadProducts() {
        try {
            this.products = await API.getAllProducts();
            console.log('Loaded products:', this.products);

            // Extract unique categories
            this.products.forEach(product => {
                if (product.category) {
                    this.categories.add(product.category);
                }
            });
        } catch (error) {
            console.error('Failed to load products:', error);
            this.showError('Failed to load products. Please refresh the page.');
        }
    }

    populateCategories() {
        const categoryFilter = document.getElementById('category-filter');
        // Clear existing options except "All Categories"
        categoryFilter.innerHTML = '<option value="">All Categories</option>';

        // Add dynamic categories
        Array.from(this.categories).sort().forEach(category => {
            const option = document.createElement('option');
            option.value = category;
            option.textContent = category;
            categoryFilter.appendChild(option);
        });
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

    checkLoginStatus() {
        const userInfo = localStorage.getItem('userInfo');
        if (userInfo) {
            this.currentUser = JSON.parse(userInfo);
            this.showAccountInfo();
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
            const isOutOfStock = product.quantity === 0 || !product.available;

            // Calculate display price (with discount if applicable)
            const displayPrice = product.discountedPrice || product.price;
            const hasDiscount = product.discountedPrice && product.discountedPrice < product.price;

            card.innerHTML = `
                <h3>${product.name}</h3>
                <p class="category">${product.category}</p>
                <p>${product.description}</p>
                <p class="price">
                    ${hasDiscount ? `<span class="original-price">$${product.price.toFixed(2)}</span>` : ''}
                    $${displayPrice.toFixed(2)}
                    ${hasDiscount ? '<span class="discount-badge">SALE</span>' : ''}
                </p>
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

            await this.loadProducts();
            this.renderProducts();

            this.showSuccess(`${product.name} added to cart!`);
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

            await this.loadProducts();
            this.renderProducts();

            this.showSuccess('Item removed from cart');
        } catch (error) {
            console.error('Remove from cart error:', error);
            this.showError('Failed to remove item: ' + error.message);
        }
    }

    showSection(sectionId) {
        document.getElementById('products').classList.add('hidden');
        document.getElementById('cart').classList.add('hidden');
        document.getElementById('account').classList.add('hidden');

        document.getElementById(sectionId).classList.remove('hidden');
    }

    showError(message) {
        const toast = document.createElement('div');
        toast.className = 'toast toast-error';
        toast.textContent = '❌ ' + message;
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.classList.add('toast-fade-out');
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

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

    // Account functionality
    showAccountInfo() {
        document.getElementById('account-login').classList.add('hidden');
        document.getElementById('account-register').classList.add('hidden');
        document.getElementById('account-info').classList.remove('hidden');

        const userInfoDiv = document.getElementById('user-info');
        userInfoDiv.innerHTML = `
            <p><strong>Name:</strong> ${this.currentUser.name}</p>
            <p><strong>Email:</strong> ${this.currentUser.email}</p>
            <p><strong>Phone:</strong> ${this.currentUser.phone}</p>
            <p><strong>Address:</strong> ${this.currentUser.address}</p>
        `;

        this.loadOrderHistory();
    }

    async loadOrderHistory() {
        const orderHistoryDiv = document.getElementById('order-history');
        orderHistoryDiv.innerHTML = '<p>Loading order history...</p>';

        try {
            const orders = await API.getOrderHistory(this.currentUser.email);

            if (!orders || orders.length === 0) {
                orderHistoryDiv.innerHTML = '<p>No orders yet</p>';
                return;
            }

            orderHistoryDiv.innerHTML = '';
            orders.forEach(order => {
                const orderDiv = document.createElement('div');
                orderDiv.className = 'order-history-item';
                orderDiv.innerHTML = `
                    <h4>Order #${order.orderId}</h4>
                    <p>Date: ${new Date(order.orderDate).toLocaleDateString()}</p>
                    <p>Total: $${order.total.toFixed(2)}</p>
                    <p>Status: ${order.status}</p>
                    ${order.trackingNumber ? `<p>Tracking: ${order.trackingNumber}</p>` : ''}
                `;
                orderHistoryDiv.appendChild(orderDiv);
            });
        } catch (error) {
            console.error('Failed to load order history:', error);
            orderHistoryDiv.innerHTML = '<p>Failed to load order history</p>';
        }
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

        // Navigation
        const productsLinks = document.querySelectorAll('a[href="#products"]');
        productsLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                this.showSection('products');
            });
        });

        document.getElementById('cart-link').addEventListener('click', (e) => {
            e.preventDefault();
            this.showSection('cart');
        });

        const accountLinks = document.querySelectorAll('a[href="#account"]');
        accountLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                this.showSection('account');
            });
        });

        // Account forms
        document.getElementById('show-register').addEventListener('click', (e) => {
            e.preventDefault();
            document.getElementById('account-login').classList.add('hidden');
            document.getElementById('account-register').classList.remove('hidden');
        });

        document.getElementById('show-login').addEventListener('click', (e) => {
            e.preventDefault();
            document.getElementById('account-register').classList.add('hidden');
            document.getElementById('account-login').classList.remove('hidden');
        });

        document.getElementById('login-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleLogin();
        });

        document.getElementById('register-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleRegister();
        });

        document.getElementById('logout-btn').addEventListener('click', () => {
            this.handleLogout();
        });

        // Checkout
        document.getElementById('checkout-btn').addEventListener('click', () => {
            if (this.cart.length === 0) {
                this.showError('Your cart is empty');
                return;
            }

            // Pre-fill if user is logged in
            if (this.currentUser) {
                document.getElementById('customer-name').value = this.currentUser.name;
                document.getElementById('customer-email').value = this.currentUser.email;
                document.getElementById('customer-phone').value = this.currentUser.phone;
                document.getElementById('customer-address').value = this.currentUser.address;
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

    handleLogin() {
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;

        // Simple mock authentication - in production, this would call the backend
        const mockUser = {
            name: 'Test User',
            email: email,
            phone: '1234567890',
            address: '123 Test St, Melbourne VIC 3000'
        };

        localStorage.setItem('userInfo', JSON.stringify(mockUser));
        this.currentUser = mockUser;
        this.showAccountInfo();
        this.showSuccess('Logged in successfully!');
    }

    handleRegister() {
        const userData = {
            name: document.getElementById('register-name').value,
            email: document.getElementById('register-email').value,
            phone: document.getElementById('register-phone').value,
            address: document.getElementById('register-address').value
        };

        localStorage.setItem('userInfo', JSON.stringify(userData));
        this.currentUser = userData;
        this.showAccountInfo();
        this.showSuccess('Account created successfully!');
    }

    handleLogout() {
        localStorage.removeItem('userInfo');
        this.currentUser = null;
        document.getElementById('account-info').classList.add('hidden');
        document.getElementById('account-login').classList.remove('hidden');
        document.getElementById('login-form').reset();
        this.showSuccess('Logged out successfully!');
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

        const submitBtn = document.querySelector('#checkout-form button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Processing...';

        try {
            console.log('Processing checkout...', customerData);
            const invoice = await API.checkout(customerData, paymentMethod);
            console.log('Checkout response:', invoice);

            if (invoice.success) {
                document.getElementById('checkout-modal').classList.add('hidden');
                this.displayInvoice(invoice);
                this.cart = [];
                this.updateCartDisplay();
                document.getElementById('checkout-form').reset();
                await this.loadProducts();
                this.renderProducts();
            } else {
                this.showError('Checkout failed: ' + (invoice.error || 'Unknown error'));
            }

        } catch (error) {
            console.error('Checkout error:', error);
            this.showError('Checkout failed: ' + error.message);
        } finally {
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
            this.showSection('products');
        };
    }
}

// Initialize app
const app = new ShopApp();